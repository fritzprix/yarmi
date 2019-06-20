package net.doodream.yarmi.net;

import net.doodream.yarmi.data.RMIError;
import net.doodream.yarmi.data.RMIServiceInfo;
import net.doodream.yarmi.data.Request;
import net.doodream.yarmi.data.Response;
import net.doodream.yarmi.serde.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;

public abstract class BaseServiceAdapter implements ServiceAdapter {

    protected static final Logger Log = LoggerFactory.getLogger(BaseServiceAdapter.class);
    private final ExecutorService executorService = Executors.newWorkStealingPool();
    private final Map<RMISocket, Future> handshakeTasks = new ConcurrentHashMap<>();
    private final Set<ClientSocketAdapter> activeClients = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private volatile boolean listen = false;
    private Future<?> clientReceptionTask;


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
        clientReceptionTask = executorService.submit(() -> {
            listen = true;
            try {
                while (listen) {
                    final RMISocket client = acceptClient();
                    handshakeTasks.put(client, executorService.submit(() -> {
                        try {
                            final RMISocket confirmedClient = negotiator.handshake(client, serviceInfo, converter, false);
                            final ClientSocketAdapter socketAdapter = ClientSocketAdapter.create(confirmedClient, converter);
                            onHandshakeSuccess(socketAdapter, handleRequest);
                        } catch (IOException e) {
                            Log.error("stop client handle {}", e.getMessage());
                        } finally {
                            handshakeTasks.remove(client);
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
        adapter.startListen(request -> {
            try {
                if (Request.isValid(request)) {
                    executorService.submit(() -> {
                        try {
                            if (Log.isTraceEnabled()) {
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
                            handleClientError(adapter, e);
                        }
                    });
                } else {
                    adapter.write(Response.from(RMIError.BAD_REQUEST));
                }
            } catch (IOException e) {
                handleClientError(adapter, e);
            }
        });
        activeClients.add(adapter);
    }

    private void handleClientError(ClientSocketAdapter adapter, Throwable throwable) {
        Log.error("Error : ", throwable);
        if(activeClients.remove(adapter)) {
            try {
                adapter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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

        cancelTask(clientReceptionTask);
        for (Map.Entry<RMISocket, Future> entry : handshakeTasks.entrySet()) {
            cancelTask(entry.getValue());
        }
    }

    private void cancelTask(Future<?> task) {
        if(task == null) {
            return;
        }

        if(task.isDone() || task.isCancelled()) {
            return;
        }
        task.cancel(true);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        close();
        executorService.shutdown();
    }

    protected abstract void onStart(InetAddress bindAddress) throws IOException;
    protected abstract boolean isClosed();
    protected abstract String getProxyConnectionHint(RMIServiceInfo serviceInfo);
    protected abstract RMISocket acceptClient() throws IOException;
    protected abstract void onClose() throws IOException;

}
