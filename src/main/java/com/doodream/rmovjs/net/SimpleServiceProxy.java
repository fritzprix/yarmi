package com.doodream.rmovjs.net;

import com.doodream.rmovjs.annotation.RMIException;
import com.doodream.rmovjs.model.*;
import com.doodream.rmovjs.net.session.BlobSession;
import com.doodream.rmovjs.net.session.SessionCommand;
import com.doodream.rmovjs.net.session.SessionControlMessage;
import com.doodream.rmovjs.serde.Converter;
import com.doodream.rmovjs.serde.Reader;
import com.doodream.rmovjs.serde.Writer;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;


public class SimpleServiceProxy implements ServiceProxy {

    private static final Logger Log = LoggerFactory.getLogger(SimpleServiceProxy.class);

    private volatile int openSemaphore;
    private volatile boolean isValid;
    private final ConcurrentHashMap<String, BlobSession> sessionRegistry = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Request> requestWaitQueue = new ConcurrentHashMap<>();
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private final AtomicInteger requestId =  new AtomicInteger(0);;
    private final RMIServiceInfo serviceInfo;
    private final RMISocket socket;
    private Converter converter;
    private Reader reader;
    private Writer writer;
    private Scheduler mListener = Schedulers.from(Executors.newWorkStealingPool(10));

    public static SimpleServiceProxy create(RMIServiceInfo info, RMISocket socket) {
        return new SimpleServiceProxy(info, socket);
    }

    private SimpleServiceProxy(RMIServiceInfo info, RMISocket socket)  {
        // set Qos as bad as possible
        Log.debug("service proxy created");
        openSemaphore = 0;
        serviceInfo = info;
        isValid = false;
        this.socket = socket;
    }

    @Override
    public synchronized boolean open() throws IOException, IllegalAccessException, InstantiationException {
        if(openSemaphore++ > 0) {
            Log.debug("already opened {} times @ {}", openSemaphore, serviceInfo.getName());
            return false;
        }

        Negotiator negotiator = (Negotiator) serviceInfo.getNegotiator().newInstance();
        converter = (Converter) serviceInfo.getConverter().newInstance();
        Log.debug("opening proxy...");
        socket.open();
        reader = converter.reader(socket.getInputStream());
        writer = converter.writer(socket.getOutputStream());
        negotiator.handshake(socket, serviceInfo, converter, true);

        Log.debug("open proxy for {} : success", serviceInfo.getName());
        isValid = true;

        compositeDisposable.add(Observable.<Response>create(emitter -> {
            try {
                while (isValid) {
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
                isValid = false;
            } finally {
                emitter.onComplete();
            }
        }).subscribeOn(mListener)
          .subscribe(response -> {
            final Request request = requestWaitQueue.get(response.getNonce());
            if (request == null) {
                Log.warn("no mapped request exists : {}", response);
                return;
            }
            request.setResponse(response);
        }, throwable -> onError(throwable)));

        return true;
    }


    @Override
    public Response request(Endpoint endpoint, long timeoutInMill, Object ...args) throws IOException {
        if(!isValid) {
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
                    // TODO: 19. 4. 25 implement timeout logic for blob exchange instead of indefinite waiting
                    long timeout = hasBlobSession? 0L : timeoutInMill;
                    try {
                        if(timeout > 0) {
                            writer.write(req, timeout, TimeUnit.MILLISECONDS);
                        } else {
                            writer.write(req);
                        }
                        return req.getResponse(timeout);

                    } catch (RMIException | InterruptedException e) {
                        return RMIError.TIMEOUT.getResponse();
                    } finally {
                        requestWaitQueue.remove(req.getNonce());
                    }
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
        close(true);
        throw new Exception(throwable);
    }

    public synchronized void close(boolean force) throws IOException {
        Log.debug("close {}", force);
        if (--openSemaphore > 0) {
            Log.debug("not close : proxy still being used by {}", openSemaphore);
            if(!force) {
                return;
            }
        }
        actualClose();
    }

    /**
     * close socket for this service proxy and stop listening to response from the remote service
     * @throws IOException try to close socket when it is already closed by peer or has not been opened at all
     */
    private void actualClose() throws IOException {
        isValid = false;
        Log.debug("actual close {}", serviceInfo);
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
                // put error response on the request
            request.setResponse(RMIError.CLOSED.getResponse());
                // wake blocked threads from wait queue
        }
        Log.debug("proxy for {} closed", serviceInfo.getName());
    }



    /**
     * check whether the resource has been used previously or not
     * @param semaphore {@link AtomicInteger} to be used as semaphore for resource
     * @return true, if the resource has been unused previously, otherwise false
     */
    private boolean markAsUse(AtomicInteger semaphore) {
        Log.debug("mark as use");
        return !(semaphore.getAndIncrement() > 0);
    }

    /**
     * check whether the resource is still used or not
     * @param semaphore {@link AtomicInteger} to be used as semaphore for resource
     * @return true, resource becomes unused, otherwise false
     */
    private boolean markAsUnuse(AtomicInteger semaphore) {
        return (semaphore.decrementAndGet() == 0);
//        return !(semaphore.updateAndGet(v -> {
//            if(v > 0) {
//                return --v;
//            }
//            return v;
//        }) > 0);
    }

    @Override
    public String who() {
        return Base64.getEncoder().encodeToString(socket.getRemoteName().concat(serviceInfo.toString()).getBytes());
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
