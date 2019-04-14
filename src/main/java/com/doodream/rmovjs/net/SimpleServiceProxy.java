package com.doodream.rmovjs.net;

import com.doodream.rmovjs.Properties;
import com.doodream.rmovjs.annotation.server.Controller;
import com.doodream.rmovjs.method.RMIMethod;
import com.doodream.rmovjs.model.*;
import com.doodream.rmovjs.net.session.BlobSession;
import com.doodream.rmovjs.net.session.SessionCommand;
import com.doodream.rmovjs.net.session.SessionControlMessage;
import com.doodream.rmovjs.serde.Converter;
import com.doodream.rmovjs.serde.Reader;
import com.doodream.rmovjs.serde.Writer;
import com.doodream.rmovjs.server.BasicService;
import com.doodream.rmovjs.server.svc.HealthCheckController;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Scheduler;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


public class SimpleServiceProxy implements ServiceProxy {

    private static final Logger Log = LoggerFactory.getLogger(SimpleServiceProxy.class);
    private static final Endpoint HEALTH_CHECK_ENDPOINT;
    static {
        Controller controller = BasicService.getHealthCheckController();
        if(controller == null) {
            HEALTH_CHECK_ENDPOINT = Endpoint.builder().build();
        } else {
            Method healthCheck = Observable.fromArray(HealthCheckController.class.getDeclaredMethods())
                    .filter(RMIMethod::isValidMethod)
                    .blockingFirst();

            HEALTH_CHECK_ENDPOINT = Endpoint.create(controller, healthCheck);
            final String healthCheckPath = Properties.getHealthCheckPath();
            if(!healthCheckPath.isEmpty()) {
                HEALTH_CHECK_ENDPOINT.setPath(Properties.getHealthCheckPath());
            }
            Log.debug("healthCheck {}", HEALTH_CHECK_ENDPOINT);
        }
    }

    private AtomicInteger openSemaphore;
    private AtomicInteger qosUpdateSemaphore;
    private Set<QosListener> qosSubscription;
    private volatile boolean isOpened;
    private final ConcurrentHashMap<String, BlobSession> sessionRegistry;
    private ConcurrentHashMap<Integer, Request> requestWaitQueue;
    private final CompositeDisposable compositeDisposable;
    private Disposable qosUpdateDisposable;
    private AtomicInteger requestId;
    private RMIServiceInfo serviceInfo;
    private Converter converter;
    private RMISocket socket;
    private Reader reader;
    private Writer writer;
    private Scheduler mListener = Schedulers.from(Executors.newWorkStealingPool(10));

    public static SimpleServiceProxy create(RMIServiceInfo info, RMISocket socket) {
        return new SimpleServiceProxy(info, socket);
    }

    private SimpleServiceProxy(RMIServiceInfo info, RMISocket socket)  {
        sessionRegistry = new ConcurrentHashMap<>();
        requestWaitQueue = new ConcurrentHashMap<>();
        compositeDisposable = new CompositeDisposable();
        // set Qos as bad as possible

        openSemaphore = new AtomicInteger(0);
        qosUpdateSemaphore = new AtomicInteger(0);
        requestId = new AtomicInteger(0);
        serviceInfo = info;
        isOpened = false;
        this.socket = socket;
    }

    @Override
    public synchronized void open() throws IOException, IllegalAccessException, InstantiationException {
        if(!markAsUse(openSemaphore)) {
            Log.debug("already opened");
            return;
        }

        Negotiator negotiator = (Negotiator) serviceInfo.getNegotiator().newInstance();
        converter = (Converter) serviceInfo.getConverter().newInstance();

        socket.open();
        reader = converter.reader(socket.getInputStream());
        writer = converter.writer(socket.getOutputStream());
        socket = negotiator.handshake(socket, serviceInfo, converter, true);

        Log.trace("open proxy for {} : success", serviceInfo.getName());
        isOpened = true;

        compositeDisposable.add(Observable.<Response>create(new ObservableOnSubscribe<Response>() {
            @Override
            public void subscribe(ObservableEmitter<Response> emitter) throws Exception {
                try {
                    while (isOpened) {
                        Response response = reader.read(Response.class);
                        if (response == null) {
                            return;
                        }
                        if (response.hasScm()) {
                            handleSessionControlMessage(response);
                            continue;
                        }
                        emitter.onNext(response);
                    }
                } catch (IOException ignore) {
                    isOpened = false;
                } finally {
                    emitter.onComplete();
                }
            }
        }).subscribeOn(mListener).subscribe(response -> {
            Request request = requestWaitQueue.remove(response.getNonce());
            if (request == null) {
                Log.warn("no mapped request exists : {}", response);
                return;
            }
            synchronized (request) {
                Log.debug("request({}) is response({})", request, response);
                request.setResponse(response);
                request.notifyAll(); // wakeup waiting thread
            }
        }, throwable -> onError(throwable)));
    }

    @Override
    public boolean isOpen() {
        return isOpened;
    }

    @Override
    public Response request(Endpoint endpoint, long timeoutInMill, Object ...args) throws IOException {
        if(!isOpen()) {
            throw new IOException("proxy closed");
        }

        final Request request = Request.fromEndpoint(endpoint, args);
        final boolean hasBlobSession = request.getSession() != null;

        return Observable.just(request)
                .doOnNext(req -> {
                    req.setNonce(requestId.incrementAndGet());
                    if(hasBlobSession) {
                        registerSession(req.getSession());
                    }
                    if(Log.isTraceEnabled()) {
                        Log.trace("Request => {}", req);
                    }
                })
                .map(req -> {
                    requestWaitQueue.put(req.getNonce(), req);

                    // blob exchange takes more time than typical timeout value, even though when it works normally
                    // so for the blob exchange, timeout value is forced to set to zero which means indefinite timeout (wait forever)
                    long timeout = hasBlobSession? 0L : timeoutInMill;

                    try {
                        synchronized (req) {
                            writer.write(req);
                            if (timeout > 0) {
                                req.wait(timeout);
                            } else {
                                req.wait();
                            }
                        }
                    } catch (InterruptedException e) {
                        return RMIError.CLOSED.getResponse();
                    }
                    if (req.getResponse() == null) {
                        return RMIError.TIMEOUT.getResponse();
                    }
                    return req.getResponse();
                })
                .doOnNext(response -> {
                    if (response.isSuccessful()) {
                        response.resolve(converter, endpoint.getUnwrappedRetType());
                    }
                })
                .map(response -> {
                    if (response.isHasSessionSwitch()) {
                        return handleBlobResponse(response);
                    } else {
                        return response;
                    }
                })
                .defaultIfEmpty(RMIError.UNHANDLED.getResponse())
                .doOnError(throwable -> onError(throwable))
                .blockingSingle();
    }

    private Response handleBlobResponse(Response response) {
        if(response.getBody() != null) {
            final BlobSession session = (BlobSession) response.getBody();
            session.init();
            if(sessionRegistry.put(session.getKey(), session) != null) {
                Log.warn("session conflict for {}", session.getKey());
            } else {
                session.start(reader, writer, converter, Request::buildSessionMessageWriter, () -> unregisterSession(session));
            }
            return response;
        }
        return RMIError.BAD_RESPONSE.getResponse();
    }

    private void handleSessionControlMessage(Response response) throws IOException, IllegalAccessException, InstantiationException, ClassNotFoundException {
        SessionControlMessage scm = response.getScm();
        BlobSession session;
        session = sessionRegistry.get(scm.getKey());
        if (session == null) {
            Log.warn("session not available for {} @ {}", scm.getCommand(), scm.getKey());
            if(scm.getCommand() == SessionCommand.ERR) {
                Log.warn("session error {}", scm.getParam());
            }
            return;
        }
        session.handle(scm);
    }

    /**
     * register session
     * @param session
     */
    private void registerSession(BlobSession session) {
        if (sessionRegistry.put(session.getKey(), session) != null) {
            Log.warn("session : {} collision in registry", session.getKey());
        }
        Log.debug("session registered {}", session);
        session.start(reader, writer, converter, Request::buildSessionMessageWriter, () -> unregisterSession(session));
    }

    private void unregisterSession(BlobSession session ) {
        if (sessionRegistry.remove(session.getKey()) == null) {
            Log.warn("fail to remove session : session not exists {}", session.getKey());
            return;
        }
        Log.trace("remove session : {}", session.getKey());
    }

    private void onError(Throwable throwable) throws Exception {
        Log.error("proxy closed {}({}) : {}", socket.getRemoteName(), serviceInfo.getName(), throwable.getMessage());
        actualClose();
        throw new Exception(throwable);
    }

    public void close() throws IOException {
        if(!markAsUnuse(openSemaphore)) {
            return;
        }
        actualClose();
    }

    /**
     * close socket for this service proxy and stop listening to response from the remote service
     * @throws IOException try to close socket when it is already closed by peer or has not been opened at all
     */
    private synchronized void actualClose() throws IOException {
        isOpened = false;
        if(qosUpdateDisposable != null) {
            if (!qosUpdateDisposable.isDisposed()) {
                qosUpdateDisposable.dispose();
            }
        }

        if(compositeDisposable != null) {
            if (!compositeDisposable.isDisposed()) {
                compositeDisposable.dispose();
            }
            compositeDisposable.clear();
        }
        if(!socket.isClosed()) {
            socket.close();
        }

        for (Request request : requestWaitQueue.values()) {
            synchronized (request) {
                // put error response on the request
                request.setResponse(RMIError.CLOSED.getResponse());
                // wake blocked threads from wait queue
                request.notifyAll();
            }
        }
        Log.debug("proxy for {} closed", serviceInfo.getName());
    }



    /**
     * check whether the resource has been used previously or not
     * @param semaphore {@link AtomicInteger} to be used as semaphore for resource
     * @return true, if the resource has been unused previously, otherwise false
     */
    private boolean markAsUse(AtomicInteger semaphore) {
        return !(semaphore.getAndIncrement() > 0);
    }

    /**
     * check whether the resource is still used or not
     * @param semaphore {@link AtomicInteger} to be used as semaphore for resource
     * @return true, resource becomes unused, otherwise false
     */
    private boolean markAsUnuse(AtomicInteger semaphore) {
        return !(semaphore.updateAndGet(v -> {
            if(v > 0) {
                return --v;
            }
            return v;
        }) > 0);
    }

    @Override
    public String who() {
        return Base64.getEncoder().encodeToString(socket.getRemoteName().concat(serviceInfo.toString()).getBytes());
    }

    @Override
    public void startQosMeasurement(long interval, long timeout, TimeUnit timeUnit, QosListener listener) {
        if(!markAsUse(qosUpdateSemaphore)) {
            Log.debug("already QoS measurement ongoing");
            qosSubscription.add(listener);
            return;
        }

        qosSubscription = new HashSet<>();
        qosUpdateDisposable = Observable.interval(0L, interval, timeUnit)
                .subscribe((l) -> {
                    long startTime = System.currentTimeMillis();
                    final Response<Long> response = request(HEALTH_CHECK_ENDPOINT, timeUnit.toMillis(timeout));
                    if(response.isSuccessful()) {
                        listener.onQosUpdated(this,System.currentTimeMillis() - startTime);
                    } else {
                        listener.onQosUpdated(this, Long.MAX_VALUE);
                    }
                }, throwable -> listener.onError(this, throwable));
    }

    @Override
    public synchronized void stopQosMeasurement(QosListener listener) {
        if(!markAsUnuse(qosUpdateSemaphore)) {
            qosSubscription.remove(listener);
            return;
        }
        qosSubscription.clear();
        if(qosUpdateDisposable == null) {
            return;
        }
        if(!qosUpdateDisposable.isDisposed()) {
            qosUpdateDisposable.dispose();
        }

        qosUpdateDisposable = null;
    }

    @Override
    public boolean provide(Class controller) {
        return Observable.fromIterable(serviceInfo.getControllerInfos())
                .map((Function<ControllerInfo, Class<?>>) controllerInfo -> controllerInfo.getStubCls())
                .map(stubCls -> controller.equals(stubCls))
                .reduce((match1, match2) -> match1 || match2)
                .blockingGet(false);
    }
}
