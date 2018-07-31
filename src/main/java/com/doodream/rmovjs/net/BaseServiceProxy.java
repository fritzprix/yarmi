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
import io.reactivex.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
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
    private AtomicInteger semaphore;
    private volatile boolean isOpened;
    private final ConcurrentHashMap<String, BlobSession> sessionRegistry;
    private ConcurrentHashMap<Integer, Request> requestWaitQueue;
    private CompositeDisposable compositeDisposable;
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

        semaphore = new AtomicInteger();
        semaphore.getAndSet(0);
        requestNonce = 0;
        serviceInfo = info;
        isOpened = false;
        this.socket = socket;
    }

    @Override
    public synchronized void open() throws IOException, IllegalAccessException, InstantiationException {
        if(semaphore.getAndAdd(1) != 0) {
            return;
        }
        Log.debug("Initialized");
        RMINegotiator negotiator = (RMINegotiator) serviceInfo.getNegotiator().newInstance();
        converter = (Converter) serviceInfo.getConverter().newInstance();
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
    public Response request(Endpoint endpoint, Object ...args) {

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
                    synchronized (request) {
                        writer.write(request);
                        // caller block here, until the response is ready
                        request.wait();
                    }
                    return Optional.of(request.getResponse());
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .defaultIfEmpty(RMIError.UNHANDLED.getResponse())
                .doOnError(this::onError)
                .doOnNext(response -> response.resolve(converter, endpoint.getUnwrappedRetType()))
                .doOnNext(response -> {
                    Log.trace("Response <= {}", response);
                    if(response.isHasSessionSwitch() &&
                    response.isSuccessful()) {
                        final BlobSession session = (BlobSession) response.getBody();
                        if(session != null) {
                            session.init();
                            if (sessionRegistry.put(session.getKey(), session) != null) {
                                Log.warn("session conflict for {}", session.getKey());
                                return;
                            }
                            session.start(reader, writer, Request::buildSessionMessageWriter, () -> unregisterSession(session));
                        }
                    }
                })
                .blockingSingle();
    }

    private void handleSessionControlMessage(Response response) throws IOException {
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
        session.start(reader, writer, Request::buildSessionMessageWriter, () -> unregisterSession(session));
    }

    private void unregisterSession(BlobSession session ) {
        if (sessionRegistry.remove(session.getKey()) == null) {
            Log.warn("fail to remove session : session not exists {}", session.getKey());
            return;
        }
        Log.trace("remove session : {}", session.getKey());
    }

    private void onError(Throwable throwable) {
        Log.error("{}", throwable);
        try {
            close();
        } catch (IOException ignored) { }
    }

    public void close() throws IOException {
        if(semaphore.getAndDecrement() != 0) {
            return;
        }
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
    public Optional<Long> ping() {
        final Response<Long> response = request(HEALTH_CHECK_ENDPOINT);
        if(response.isSuccessful() && (response.getCode() == Response.SUCCESS)) {
            Log.debug("body {} /w cls {}", response.getBody(), response.getBody().getClass());
            return Optional.of(response.getBody());
        }
        return Optional.empty();
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
