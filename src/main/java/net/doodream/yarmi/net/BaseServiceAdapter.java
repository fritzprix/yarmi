package net.doodream.yarmi.net;

import net.doodream.yarmi.model.RMIError;
import net.doodream.yarmi.model.RMIServiceInfo;
import net.doodream.yarmi.model.Request;
import net.doodream.yarmi.model.Response;
import net.doodream.yarmi.serde.Converter;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;

public abstract class BaseServiceAdapter implements ServiceAdapter {

    protected static final Logger Log = LoggerFactory.getLogger(BaseServiceAdapter.class);
    private final ExecutorService executorService = Executors.newWorkStealingPool();
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private final Map<RMISocket, Future> clientTasks = new ConcurrentHashMap<>();

    private volatile boolean listen = false;
    private Future<?> listenTask;

    @Override
    public synchronized String listen(final RMIServiceInfo serviceInfo, final InetAddress network, final Function<Request, Response> handleRequest) throws IllegalAccessException, InstantiationException, IOException {
        if(listen) {
            throw new IllegalStateException("service already listening");
        }
        if(executorService.isTerminated() || executorService.isShutdown()) {
            throw new IllegalStateException("stopped service can't be used again");
        }

        if(network == null) {
            throw new IllegalArgumentException("network can't be null");
        }

        final Negotiator negotiator = (Negotiator) serviceInfo.getNegotiator().newInstance();
        final Converter converter = (Converter) serviceInfo.getConverter().newInstance();
        onStart(network);
        listenTask = executorService.submit(() -> {
            listen = true;
            try {
                while (listen) {
                    final RMISocket client = acceptClient();
                    clientTasks.put(client, executorService.submit(() -> {
                        try {
                            final RMISocket confirmedClient = negotiator.handshake(client, serviceInfo, converter, false);
                            final ClientSocketAdapter socketAdapter = ClientSocketAdapter.create(confirmedClient, converter);
                            onHandshakeSuccess(socketAdapter, handleRequest);
                        } catch (IOException e) {
                            Log.error("stop client handle {}", e.getMessage());
                        } finally {
                            clientTasks.remove(client);
                        }
                    }));
                }
            } catch (IOException e) {
                Log.warn("stop service : {}", e.getMessage());
            }
        });

        return getProxyConnectionHint(serviceInfo);
    }

    private void onHandshakeSuccess(final ClientSocketAdapter adapter, final Function<Request, Response> handleRequest) {
        compositeDisposable.add(adapter.listen()
                .subscribeOn(Schedulers.computation())
                .subscribe(request -> {
            if(Request.isValid(request)) {
                executorService.submit(() -> {
                    try {
                        if(Log.isTraceEnabled()) {
                            Log.trace("Request <= {}", request);
                        }
                        request.setClient(adapter);
                        try {
                            final Response response = handleRequest.apply(request);
                            if (Log.isTraceEnabled()) {
                                Log.trace("Response => {}", response);
                            }
                            adapter.write(response);
                        } catch (Exception e) {
                            adapter.write(RMIError.INTERNAL_SERVER_ERROR.getResponse());
                        }
                    } catch (Exception e) {
                        onError(e);
                    }
                });
            } else {
                adapter.write(Response.from(RMIError.BAD_REQUEST));
            }
        }));
    }

    private void onError(Throwable throwable) {
        Log.error("Error : ", throwable);
        close();
    }

    @Override
    public void close() {
        listen = false;
        if(!isClosed()) {
            try {
                onClose();
            } catch (IOException e) {
                Log.warn("", e);
            }
        }
        compositeDisposable.dispose();
        compositeDisposable.clear();

        executorService.shutdown();
    }


    protected abstract void onStart(InetAddress bindAddress) throws IOException;
    protected abstract boolean isClosed();
    protected abstract String getProxyConnectionHint(RMIServiceInfo serviceInfo);
    protected abstract RMISocket acceptClient() throws IOException;
    protected abstract void onClose() throws IOException;

}
