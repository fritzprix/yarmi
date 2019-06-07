package com.doodream.rmovjs.net;

import com.doodream.rmovjs.model.RMIError;
import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.model.Request;
import com.doodream.rmovjs.model.Response;
import com.doodream.rmovjs.serde.Converter;
import com.google.common.base.Preconditions;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.BooleanSupplier;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class BaseServiceAdapter implements ServiceAdapter {

    protected static final Logger Log = LoggerFactory.getLogger(BaseServiceAdapter.class);
    private final ExecutorService executorService = Executors.newWorkStealingPool();
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private volatile boolean listen = false;

    @Override
    public String listen(final RMIServiceInfo serviceInfo, final Converter converter, final InetAddress network, final Function<Request, Response> handleRequest) throws IllegalAccessException, InstantiationException, IOException {
        if(listen) {
            throw new IllegalStateException("service already listening");
        }
        if(executorService.isTerminated() || executorService.isShutdown()) {
            throw new IllegalStateException("stopped service can't be used again");
        }
        final Negotiator negotiator = (Negotiator) serviceInfo.getNegotiator().newInstance();
        Preconditions.checkNotNull(negotiator, "fail to resolve %s", serviceInfo.getNegotiator());
        Preconditions.checkNotNull(converter, "fail to resolve %s", serviceInfo.getConverter());
        Preconditions.checkNotNull(network, "no network interface given");

        onStart(network);

        listen = true;
        compositeDisposable.add(Observable.just(converter)
                .map(new Function<Converter, RMISocket>() {
                    @Override
                    public RMISocket apply(Converter converter) throws Exception {
                        return acceptClient();
                    }
                })
                .repeatUntil(new BooleanSupplier() {
                    @Override
                    public boolean getAsBoolean() throws Exception {
                        return !listen;
                    }
                })
                .map(new Function<RMISocket, RMISocket>() {
                    @Override
                    public RMISocket apply(RMISocket socket) throws Exception {
                        Log.debug("{} connected", socket.getRemoteName());
                        return negotiator.handshake(socket, serviceInfo, converter, false);
                    }
                })
                .map(new Function<RMISocket, ClientSocketAdapter>() {
                    @Override
                    public ClientSocketAdapter apply(RMISocket rmiSocket) throws Exception {
                        return new ClientSocketAdapter(rmiSocket, converter);
                    }
                })
                .subscribeOn(Schedulers.from(executorService))
                .subscribe(new Consumer<ClientSocketAdapter>() {
                    @Override
                    public void accept(ClientSocketAdapter clientSocketAdapter) throws Exception {
                        onHandshakeSuccess(clientSocketAdapter, handleRequest);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        onError(throwable);
                    }
                }));

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
