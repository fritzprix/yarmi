package com.doodream.rmovjs.net;

import com.doodream.rmovjs.model.*;
import com.doodream.rmovjs.net.session.BlobSession;
import com.doodream.rmovjs.net.session.SessionControlMessage;
import com.doodream.rmovjs.serde.Converter;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.concurrent.ConcurrentHashMap;


public class BaseServiceProxy implements RMIServiceProxy {

    private static final Logger Log = LogManager.getLogger(BaseServiceProxy.class);

    private volatile boolean isOpened;
    private ConcurrentHashMap<String, BlobSession> sessionRegistry;
    private ConcurrentHashMap<Integer, Request> requestWaitQueue;
    private CompositeDisposable compositeDisposable;
    private int requestNonce;
    private RMIServiceInfo serviceInfo;
    private RMISocket socket;
    private Converter converter;
    private Reader reader;
    private Writer writer;


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
        this.converter = (Converter) serviceInfo.getConverter().newInstance();
        socket.open();
        reader = converter.reader(socket.getInputStream());
        writer = converter.writer(socket.getOutputStream());
        socket = negotiator.handshake(socket, serviceInfo, converter, true);
        Log.info("open proxy for {} : success", serviceInfo.getName());
        isOpened = true;

        compositeDisposable.add(Observable.<Response>create(emitter -> {
            while (isOpened) {
                Response response = converter.read(reader, Response.class);
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
        }).subscribeOn(Schedulers.io()).subscribe(response -> {
            Request request = requestWaitQueue.remove(response.getNonce());
            if(request == null) {
                Log.warn("no mapped request exists : {}", response);
                return;
            }
            synchronized (request) {
                request.setResponse(response);
                request.notifyAll(); // wakeup waiting thread
            }
        }));
    }

    @Override
    public boolean isOpen() {
        return isOpened;
    }

    @Override
    public Response request(Endpoint endpoint, Object ...args) {

        return Observable.just(endpoint.toRequest(args))
                .doOnNext(request -> request.setNonce(++requestNonce))
                .doOnNext(this::registerSession)
                .doOnNext(request -> Log.debug("request => {}", request))
                .doOnNext(request -> converter.write(request, writer))
                .map(request -> {
                    requestWaitQueue.put(request.getNonce(), request);
                    synchronized (request) {
                        request.wait();
                    }
                    return request.getResponse();
                })
                .doOnError(this::onError)
                .subscribeOn(Schedulers.io())
                .blockingSingle();
    }

    private void handleSessionControlMessage(Response response) {
        SessionControlMessage scm = response.getScm();
        BlobSession session = sessionRegistry.get(scm.getKey());
        if(session == null) {
            return;
        }
        session.handle(scm);
    }

    private void registerSession(Request request) {
        BlobSession session = request.getSession();
        if(session == null) {
            return;
        }
        if(sessionRegistry.put(session.getKey(), session) != null){
            Log.warn("session : {} collision in registry", session.getKey());
        }
        session.start(reader, Request.buildSessionMessageWriter(writer), () -> {
            if(sessionRegistry.remove(session.getKey()) != null) {
                Log.warn("fail to remove session : session not exists {}", session.getKey());
            }
        });
    }

    private void onError(Throwable throwable) {
        Log.error(throwable);
        try {
            close();
        } catch (IOException ignored) { }
    }

    public void close() throws IOException {
        if(!isOpened) {
            return;
        }
        Log.debug("proxy for {} closed", serviceInfo.getName());
        if(socket.isClosed()) {
            return;
        }
        socket.close();
        isOpened = false;
        compositeDisposable.dispose();
        compositeDisposable.clear();
    }

    @Override
    public boolean provide(Class controller) {
        return Observable.fromIterable(serviceInfo.getControllerInfos())
                .map(ControllerInfo::getStubCls)
                .map(controller::equals)
                .blockingFirst(false);
    }
}
