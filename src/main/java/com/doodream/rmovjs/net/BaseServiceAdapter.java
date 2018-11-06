package com.doodream.rmovjs.net;

import com.doodream.rmovjs.model.RMIError;
import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.model.Request;
import com.doodream.rmovjs.model.Response;
import com.doodream.rmovjs.serde.Converter;
import com.google.common.base.Preconditions;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.BooleanSupplier;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.observables.GroupedObservable;
import io.reactivex.schedulers.Schedulers;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public abstract class BaseServiceAdapter implements ServiceAdapter {

    protected static final Logger Log = LoggerFactory.getLogger(BaseServiceAdapter.class);
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private volatile boolean listen = false;

    @Override
    public String listen(final RMIServiceInfo serviceInfo, final Converter converter, @NonNull final Function<Request, Response> handleRequest) throws IllegalAccessException, InstantiationException, IOException {
        if(listen) {
            throw new IllegalStateException("service already listening");
        }
        final RMINegotiator negotiator = (RMINegotiator) serviceInfo.getNegotiator().newInstance();
        Preconditions.checkNotNull(negotiator, "fail to resolve %s", serviceInfo.getNegotiator());
        Preconditions.checkNotNull(converter, "fail to resolve %s", serviceInfo.getConverter());
        onStart();

        listen = true;
        compositeDisposable.add(Observable.just(converter)
                .map(new Function<Converter, RMISocket>() {
                    @Override
                    public RMISocket apply(Converter converter) throws Exception {
                        return acceptClient();
                    }
                })
                .doOnNext(new Consumer<RMISocket>() {
                    @Override
                    public void accept(RMISocket socket) throws Exception {
                        Log.debug("{} connected", socket.getRemoteName());
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
                    public RMISocket apply(RMISocket rmiSocket) throws Exception {
                        return negotiator.handshake(rmiSocket, serviceInfo, converter, false);
                    }
                })
                .map(new Function<RMISocket, ClientSocketAdapter>() {
                    @Override
                    public ClientSocketAdapter apply(RMISocket rmiSocket) throws Exception {
                        return new ClientSocketAdapter(rmiSocket, converter);
                    }
                })
                .subscribeOn(Schedulers.newThread())
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
                .groupBy(new Function<Request, Boolean>() {
                    @Override
                    public Boolean apply(Request request) throws Exception {
                        return Request.isValid(request);
                    }
                })
                .flatMap(new Function<GroupedObservable<Boolean, Request>, ObservableSource<Request>>() {
                    @Override
                    public ObservableSource<Request> apply(final GroupedObservable<Boolean, Request> booleanRequestGroupedObservable) throws Exception {
                        return Observable.create(new ObservableOnSubscribe<Request>() {
                            @Override
                            public void subscribe(final ObservableEmitter<Request> emitter) throws Exception {
                                if(booleanRequestGroupedObservable.getKey()) {
                                    emitter.setDisposable(booleanRequestGroupedObservable.subscribe(new Consumer<Request>() {
                                                @Override
                                                public void accept(Request request) throws Exception {
                                                    emitter.onNext(request);
                                                }
                                            }));
                                } else {
                                    // bad request handle added
                                    emitter.setDisposable(booleanRequestGroupedObservable.subscribe(new Consumer<Request>() {
                                        @Override
                                        public void accept(Request request) throws Exception {
                                            adapter.write(Response.from(RMIError.BAD_REQUEST));
                                        }
                                    }));
                                }
                            }
                        });
                    }
                })
                .observeOn(Schedulers.io())
                .subscribe(new Consumer<Request>() {
                    @Override
                    public void accept(Request request) throws Exception {
                        if(Log.isTraceEnabled()) {
                            Log.trace("Request <= {}", request);
                        }
                        request.setClient(adapter);
                        final Response response = handleRequest.apply(request);
                        if(Log.isTraceEnabled()) {
                            Log.trace("Response => {}", response);
                        }
                        adapter.write(response);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        onError(throwable);
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
    }


    protected abstract void onStart() throws IOException;
    protected abstract boolean isClosed();
    protected abstract String getProxyConnectionHint(RMIServiceInfo serviceInfo);
    protected abstract RMISocket acceptClient() throws IOException;
    protected abstract void onClose() throws IOException;

}
