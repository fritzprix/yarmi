package com.doodream.rmovjs.net;

import com.doodream.rmovjs.Properties;
import com.doodream.rmovjs.annotation.server.Controller;
import com.doodream.rmovjs.method.RMIMethod;
import com.doodream.rmovjs.model.*;
import com.doodream.rmovjs.net.session.BlobSession;
import com.doodream.rmovjs.net.session.SessionControlMessage;
import com.doodream.rmovjs.serde.Converter;
import com.doodream.rmovjs.serde.Reader;
import com.doodream.rmovjs.serde.Writer;
import com.doodream.rmovjs.server.BasicService;
import com.doodream.rmovjs.server.svc.HealthCheckController;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


public class BaseServiceProxy implements RMIServiceProxy {

    private static final Logger Log = LoggerFactory.getLogger(BaseServiceProxy.class);

    // endpoint for health check
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
    private AtomicInteger pingSemaphore;
    private long measuredQos;
    private volatile boolean isOpened;
    private final ConcurrentHashMap<String, BlobSession> sessionRegistry;
    private ConcurrentHashMap<Integer, Request> requestWaitQueue;
    private CompositeDisposable compositeDisposable;
    private Disposable pingDisposable;
    private int requestNonce;
    private RMIServiceInfo serviceInfo;
    private Converter converter;
    private RMISocket socket;
    private Reader reader;
    private Writer writer;
    private Scheduler mListener = Schedulers.from(Executors.newWorkStealingPool(10));


    public static BaseServiceProxy create(RMIServiceInfo info, RMISocket socket) {
        return new BaseServiceProxy(info, socket);
    }

    private BaseServiceProxy(RMIServiceInfo info, RMISocket socket)  {
        sessionRegistry = new ConcurrentHashMap<>();
        requestWaitQueue = new ConcurrentHashMap<>();
        compositeDisposable = new CompositeDisposable();
        pingDisposable = null;
        // set Qos as bad as possible
        measuredQos = Long.MAX_VALUE;

        openSemaphore = new AtomicInteger(0);
        pingSemaphore = new AtomicInteger(0);
        requestNonce = 0;
        serviceInfo = info;
        isOpened = false;
        this.socket = socket;
    }

    @Override
    public synchronized void open() throws IOException, IllegalAccessException, InstantiationException {
        if(!markAsUse(openSemaphore)) {
            return;
        }
        Log.debug("Initialized");
        RMINegotiator negotiator = (RMINegotiator) serviceInfo.getNegotiator().newInstance();
        converter = (Converter) serviceInfo.getConverter().newInstance();
        // TODO: 18. 8. 1

        socket.open();
        reader = converter.reader(socket.getInputStream());
        writer = converter.writer(socket.getOutputStream());
        socket = negotiator.handshake(socket, serviceInfo, converter, true);

        Log.trace("open proxy for {} : success", serviceInfo.getName());
        isOpened = true;

        compositeDisposable.add(Observable.<Response>create(emitter -> {
            while (isOpened) {
                Response response = reader.read(Response.class);
                if(response == null) {
                    return;
                }
                if(response.hasScm()) {
                    handleSessionControlMessage(response);
                    continue;
                }
                emitter.onNext(response);
            }
            emitter.onComplete();
        }).subscribeOn(mListener).subscribe(response -> {
            Request request = requestWaitQueue.remove(response.getNonce());
            if(request == null) {
                Log.warn("no mapped request exists : {}", response);
                return;
            }
            synchronized (request) {
                request.setResponse(response);
                request.notifyAll(); // wakeup waiting thread
            }
        }, this::onError));
    }

    @Override
    public boolean isOpen() {
        return isOpened;
    }

    @Override
    public Response request(Endpoint endpoint, long timeoutInMillisec, Object ...args) {

        return Observable.just(Request.fromEndpoint(endpoint, args))
                .doOnNext(request -> request.setNonce(++requestNonce))
                .doOnNext(request -> {
                    final BlobSession session = request.getSession();
                    if(session != null) {
                        registerSession(session);
                    }
                })
                .doOnNext(request -> Log.trace("Request => {}", request))
                .map(request -> {
                    requestWaitQueue.put(request.getNonce(), request);
                    try {
                        synchronized (request) {
                            writer.write(request);
                            if(timeoutInMillisec > 0) {
                                request.wait(timeoutInMillisec);
                            } else {
                                request.wait();
                            }
                        }
                    } catch (InterruptedException e) {
                        return Optional.of(RMIError.TIMEOUT.getResponse());
                    }
                    return Optional.of(request.getResponse());
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .doOnNext(response -> {
                    if(response.isSuccessful()) {
                        response.resolve(converter, endpoint.getUnwrappedRetType());
                    }
                })
                .map(response -> {
                    if(response.isHasSessionSwitch() && response.isSuccessful()) {
                        return handleBlobResponse(response);
                    } else {
                        return response;
                    }
                })
                .defaultIfEmpty(RMIError.UNHANDLED.getResponse())
                .doOnError(this::onError)
                .blockingSingle();
    }

    private Response handleBlobResponse(Response response) {
        if(response.getBody() != null) {
            try {
                response.resolve(converter, BlobSession.class);
                final BlobSession session = (BlobSession) response.getBody();
                if(sessionRegistry.put(session.getKey(), session) != null) {
                    Log.warn("session conflict for {}", session.getKey());
                } else {
                    session.start(reader, writer, converter, Request::buildSessionMessageWriter, () -> unregisterSession(session));
                }
                return response;
            } catch (IllegalAccessException | InstantiationException | ClassNotFoundException e) {
                Response errResp = RMIError.BAD_RESPONSE.getResponse();
                errResp.setBody(e.getMessage());
                return errResp;
            }
        }
        return RMIError.BAD_RESPONSE.getResponse();
    }

    private void handleSessionControlMessage(Response response) throws IOException, IllegalAccessException, InstantiationException, ClassNotFoundException {
        SessionControlMessage scm = response.getScm();
        BlobSession session;
        session = sessionRegistry.get(scm.getKey());
        if (session == null) {
            Log.warn("Session not available for {}", scm);
            return;
        }
        session.handle(scm);
    }

    private void registerSession(BlobSession session) {
        if (sessionRegistry.put(session.getKey(), session) != null) {
            Log.warn("session : {} collision in registry", session.getKey());
        }
        Log.trace("session registered {}", session);
        session.start(reader, writer, converter, Request::buildSessionMessageWriter, () -> unregisterSession(session));
    }

    private void unregisterSession(BlobSession session ) {
        if (sessionRegistry.remove(session.getKey()) == null) {
            Log.warn("fail to remove session : session not exists {}", session.getKey());
            return;
        }
        Log.trace("remove session : {}", session.getKey());
    }

    private void onError(Throwable throwable) {
        Log.error("Proxy closed {}", throwable);
        try {
            close();
        } catch (IOException ignored) { }
    }

    public void close() throws IOException {
        if(!markAsUnuse(openSemaphore)) {
            return;
        }
        stopPeriodicQosUpdate();
        if(!socket.isClosed()) {
            socket.close();
        }
        if(!compositeDisposable.isDisposed()) {
            compositeDisposable.dispose();
        }
        compositeDisposable.clear();
        // wake blocked thread from wait queue
        requestWaitQueue.values().forEach(request -> {
            synchronized (request) {
                request.notifyAll();
            }
        });
        isOpened = false;
        Log.debug("proxy for {} closed", serviceInfo.getName());
    }

    @Override
    public void startPeriodicQosUpdate(long timeout, long interval, TimeUnit timeUnit) {
        if(!markAsUse(pingSemaphore))  {
            return;
        }
        pingDisposable = Observable.interval(interval, timeUnit)
                .subscribeOn(Schedulers.io())
                .subscribe(aLong -> getQosUpdate(timeout));
    }

    @Override
    public void stopPeriodicQosUpdate() {
        if(!markAsUnuse(pingSemaphore)) {
            return;
        }
        if(!pingDisposable.isDisposed()) {
            pingDisposable.dispose();
        }
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
    public Long getQosUpdate(long timeout) {
        long sTime = System.currentTimeMillis();
        final Response response = request(HEALTH_CHECK_ENDPOINT, timeout);
        if (response.isSuccessful() && (response.getCode() == Response.SUCCESS)) {
            measuredQos = System.currentTimeMillis() - sTime;
        } else {
            measuredQos = Long.MAX_VALUE;
        }
        return measuredQos;
    }

    @Override
    public Long getQosMeasured() {
        return measuredQos;
    }


    @Override
    public String who() {
        return Base64.getEncoder().encodeToString(socket.getRemoteName().concat(serviceInfo.toString()).getBytes());
    }

    @Override
    public boolean provide(Class controller) {
        return Observable.fromIterable(serviceInfo.getControllerInfos())
                .map(ControllerInfo::getStubCls)
                .map(controller::equals)
                .reduce((isThere1, isThere2) -> isThere1 || isThere2)
                .blockingGet(false);
    }
}
