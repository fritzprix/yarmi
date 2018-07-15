package com.doodream.rmovjs.net;

import com.doodream.rmovjs.model.*;
import com.doodream.rmovjs.net.session.BlobSession;
import com.doodream.rmovjs.net.session.SessionControlMessage;
import com.doodream.rmovjs.serde.Converter;
import com.doodream.rmovjs.serde.Reader;
import com.doodream.rmovjs.serde.Writer;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;


public class BaseServiceProxy implements RMIServiceProxy {

    private static final Logger Log = LoggerFactory.getLogger(BaseServiceProxy.class);

    private volatile boolean isOpened;
    private final ConcurrentHashMap<String, BlobSession> sessionRegistry;
    private ConcurrentHashMap<Integer, Request> requestWaitQueue;
    private CompositeDisposable compositeDisposable;
    private int requestNonce;
    private RMIServiceInfo serviceInfo;
    private RMISocket socket;
    private Reader reader;
    private Writer writer;
    private Scheduler mListener = Schedulers.from(Executors.newWorkStealingPool(10));


    public static BaseServiceProxy create(RMIServiceInfo info, RMISocket socket)  {
        return new BaseServiceProxy(info, socket);
    }

    private BaseServiceProxy(RMIServiceInfo info, RMISocket socket)  {
        sessionRegistry = new ConcurrentHashMap<>();
        requestWaitQueue = new ConcurrentHashMap<>();
        compositeDisposable = new CompositeDisposable();
        requestNonce = 0;
        serviceInfo = info;
        isOpened = false;
        this.socket = socket;
    }

    @Override
    public synchronized void open() throws IOException, IllegalAccessException, InstantiationException {
        if(isOpened) {
            return;
        }
        RMINegotiator negotiator = (RMINegotiator) serviceInfo.getNegotiator().newInstance();
        Converter converter = (Converter) serviceInfo.getConverter().newInstance();
        socket.open();
        reader = converter.reader(socket.getInputStream());
        writer = converter.writer(socket.getOutputStream());
        socket = negotiator.handshake(socket, serviceInfo, converter, true);
        Log.info("open proxy for {} : success", serviceInfo.getName());
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
                .doOnNext(request -> Log.debug("Request => {}", request))
                .map(request -> {
                    requestWaitQueue.put(request.getNonce(), request);
                    synchronized (request) {
                        writer.write(request);
                        // caller block here, until the response is ready
                        request.wait();
                    }
                    return Optional.ofNullable(request.getResponse());
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .doOnNext(response -> {
                    if(response.isHasSessionSwitch()) {
                        final BlobSession session = response.getSession();
                        if(session != null) {
                            response.setBody(session);
                            session.init();
                            if (sessionRegistry.put(session.getKey(), session) != null) {
                                Log.warn("session conflict for {}", session.getKey());
                                return;
                            }
                            session.start(reader, writer, Request::buildSessionMessageWriter, () -> unregisterSession(session));
                        }
                    }
                })
                .defaultIfEmpty(RMIError.UNHANDLED.getResponse())
                .doOnError(this::onError)
                .blockingSingle();
    }

    private void handleSessionControlMessage(Response response) throws IOException {
        SessionControlMessage scm = response.getScm();
        BlobSession session;
        session = sessionRegistry.get(scm.getKey());
        if (session == null) {
            Log.debug("Session not available for {}", scm);
            return;
        }
        session.handle(scm);
    }

    private void registerSession(BlobSession session) {
        if (sessionRegistry.put(session.getKey(), session) != null) {
            Log.warn("session : {} collision in registry", session.getKey());
        }
        Log.debug("session registered {}", session);
        session.start(reader, writer, Request::buildSessionMessageWriter, () -> unregisterSession(session));
    }

    private void unregisterSession(BlobSession session ) {
        if (sessionRegistry.remove(session.getKey()) == null) {
            Log.warn("fail to remove session : session not exists {}", session.getKey());
            return;
        }
        // TODO : allow other request
        Log.debug("remove session : {}", session.getKey());
    }

    private void onError(Throwable throwable) {
        Log.error("{}", throwable);
        try {
            close();
        } catch (IOException ignored) { }
    }

    public void close() throws IOException {
        if(!isOpened) {
            return;
        }
        if(socket.isClosed()) {
            return;
        }
        socket.close();
        isOpened = false;
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
        Log.debug("proxy for {} closed", serviceInfo.getName());
    }

    @Override
    public long ping() {
        return 0;
    }

    @Override
    public boolean provide(Class controller) {
        return Observable.fromIterable(serviceInfo.getControllerInfos())
                .map(ControllerInfo::getStubCls)
                .map(controller::equals)
                .blockingFirst(false);
    }
}
