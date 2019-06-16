package net.doodream.yarmi.net;

import net.doodream.yarmi.annotation.RMIException;
import net.doodream.yarmi.model.*;
import net.doodream.yarmi.net.session.BlobSession;
import net.doodream.yarmi.net.session.SessionCommand;
import net.doodream.yarmi.net.session.SessionControlMessage;
import net.doodream.yarmi.serde.Converter;
import net.doodream.yarmi.serde.Reader;
import net.doodream.yarmi.serde.Writer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


class DefaultServiceProxy implements ServiceProxy {

    private static final Logger Log = LoggerFactory.getLogger(DefaultServiceProxy.class);

    private volatile int openSemaphore;
    private volatile boolean isValid;
    private final ConcurrentHashMap<String, BlobSession> sessionRegistry = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Request> requestWaitQueue = new ConcurrentHashMap<>();
    private final AtomicInteger requestId =  new AtomicInteger(0);
    private final RMIServiceInfo serviceInfo;
    private final RMISocket socket;
    private final ExecutorService executorService;
    private Converter converter;
    private Reader reader;
    private Writer writer;
    private Future<?> readerTask;

    public static DefaultServiceProxy create(RMIServiceInfo info, RMISocket socket) {
        return new DefaultServiceProxy(info, socket);
    }

    private DefaultServiceProxy(RMIServiceInfo info, RMISocket socket)  {
        // set Qos as bad as possible
        Log.debug("service proxy created");
        openSemaphore = 0;
        serviceInfo = info;
        isValid = false;
        this.socket = socket;
        executorService = Executors.newCachedThreadPool();
    }

    @Override
    public synchronized boolean open() throws IOException, IllegalAccessException, InstantiationException {
        if(openSemaphore++ > 0) {
            Log.debug("already opened {} times @ {}", openSemaphore, serviceInfo.getName());
            return false;
        }

        Negotiator negotiator = (Negotiator) serviceInfo.getNegotiator().newInstance();
        converter = (Converter) serviceInfo.getConverter().newInstance();
        socket.open();
        reader = converter.reader(socket.getInputStream());
        writer = converter.writer(socket.getOutputStream());
        negotiator.handshake(socket, serviceInfo, converter, true);

        Log.debug("open proxy for {} : success", serviceInfo.getName());
        isValid = true;

        readerTask = executorService.submit(() -> {
            try {
                while(isValid) {
                    final Response response = reader.read(Response.class);
                    if(response == null) {
                        continue;
                    }
                    if(response.hasScm()) {
                        handleSessionControlMessage(response);
                        continue;
                    }
                    final Request request = requestWaitQueue.get(response.getNonce());
                    if (request == null) {
                        Log.warn("no mapped request exists : {}", response);
                        return;
                    }
                    request.setResponse(response);
                }
            } catch (IOException | IllegalAccessException | InstantiationException | ClassNotFoundException e) {
                Log.warn("proxy stopped : {}", e.getMessage());
            } finally {

            }
        });
        return true;
    }


    @Override
    public Response request(Endpoint endpoint, long timeoutInMill, Object ...args) throws IOException {
        if(!isValid) {
            throw new IOException("proxy closed");
        }

        final Request request = Request.fromEndpoint(endpoint, args);
        final boolean hasBlobSession = request.getSession() != null;

        request.setNonce(requestId.incrementAndGet());
        if(hasBlobSession) {
            registerSession(request.getSession());
        }
        if(Log.isTraceEnabled()) {
            Log.trace("Request => {}", request);
        }
        requestWaitQueue.put(request.getNonce(), request);
        long timeout = hasBlobSession? 0L : timeoutInMill;
        Response response;
        try {
            if (timeout > 0) {
                writer.write(request, timeout, TimeUnit.MILLISECONDS);
            } else {
                writer.write(request);
            }
            response = request.getResponse(timeout);
        } catch (RMIException | TimeoutException e) {
            response = RMIError.TIMEOUT.getResponse();
        } finally {
            requestWaitQueue.remove(request.getNonce());
        }

        try {
            if (response.isSuccessful()) {
                response.resolve(converter, endpoint.getUnwrappedRetType());
                if (response.hasSessionSwitch()) {
                    response = handleBlobResponse(response);
                }
            }
        } catch (IllegalAccessException | InstantiationException | ClassNotFoundException e) {
            Log.warn("fail to resolve response");
            response = RMIError.BAD_RESPONSE.getResponse();
        }
        return response;



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
        if(!socket.isClosed()) {
            socket.close();
        }
        cancelReaderTask();
        executorService.shutdown();
        for (Request request : requestWaitQueue.values()) {
                // put error response on the request
            request.setResponse(RMIError.CLOSED.getResponse());
                // wake blocked threads from wait queue
        }
        Log.debug("proxy for {} closed", serviceInfo.getName());
    }

    private void cancelReaderTask() {
        if(readerTask == null) {
            return;
        }
        if(readerTask.isDone() || readerTask.isDone()) {
            return;
        }
        readerTask.cancel(true);
    }

    @Override
    public String who() {
        return Base64.getEncoder().encodeToString(socket.getRemoteName().concat(serviceInfo.toString()).getBytes());
    }

    @Override
    public boolean provide(Class controller) {
        boolean result = false;
        for (ControllerInfo controllerInfo : serviceInfo.getControllerInfos()) {
            Class stubCls = controllerInfo.getStubCls();
            result |= controller.equals(stubCls);
        }
        return result;
    }
}
