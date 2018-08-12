package com.doodream.rmovjs.net;

import com.doodream.rmovjs.model.RMIError;
import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.model.Request;
import com.doodream.rmovjs.model.Response;
import com.doodream.rmovjs.serde.Converter;
import com.google.common.base.Preconditions;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

public abstract class BaseServiceAdapter implements ServiceAdapter {

    protected static final Logger Log = LoggerFactory.getLogger(BaseServiceAdapter.class);
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private volatile boolean listen = false;

    @Override
    public String listen(RMIServiceInfo serviceInfo, Converter converter, @NonNull Function<Request, Response> handleRequest) throws IllegalAccessException, InstantiationException, IOException {
        if(listen) {
            throw new IllegalStateException("service already listening");
        }
        final RMINegotiator negotiator = (RMINegotiator) serviceInfo.getNegotiator().newInstance();
        Preconditions.checkNotNull(negotiator, "fail to resolve %s", serviceInfo.getNegotiator());
        Preconditions.checkNotNull(converter, "fail to resolve %s", serviceInfo.getConverter());
        onStart();

        listen = true;
        Observable.just(converter)
                .map(c -> acceptClient())
                .doOnNext(socket -> Log.debug("{} connected", socket.getRemoteName()))
                .repeatUntil(() -> !listen)
                .map(client -> negotiator.handshake(client, serviceInfo, converter, false))
                .map(socket -> new ClientSocketAdapter(socket, converter))
                .subscribeOn(Schedulers.newThread())
                .subscribeWith(new Observer<ClientSocketAdapter>() {

                    private ClientSocketAdapter adapter;

                    @Override
                    public void onSubscribe(Disposable disposable) {
                        compositeDisposable.add(disposable);
                    }

                    @Override
                    public void onNext(ClientSocketAdapter clientSocketAdapter) {
                        adapter = clientSocketAdapter;
                        onHandshakeSuccess(adapter, handleRequest);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        BaseServiceAdapter.this.onError(throwable, adapter);
                    }

                    @Override
                    public void onComplete() {
                        try {
                            adapter.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });

        return getProxyConnectionHint(serviceInfo);
    }

    private void onHandshakeSuccess(ClientSocketAdapter adapter, Function<Request, Response> handleRequest) {


        compositeDisposable.add(adapter.listen()
                .groupBy(Request::isValid)
                .flatMap(booleanRequestGroupedObservable -> Observable.<Optional<Request>>create(emitter -> {
                    if(booleanRequestGroupedObservable.getKey()) {
                        emitter.setDisposable(booleanRequestGroupedObservable.subscribe(request -> emitter.onNext(Optional.of(request))));
                    } else {
                        // bad request handle added
                        emitter.setDisposable(booleanRequestGroupedObservable.subscribe(request -> {
                            adapter.write(Response.from(RMIError.BAD_REQUEST));
                            emitter.onNext(Optional.empty());
                        }));
                    }
                }))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .doOnNext(request -> request.setClient(adapter))
                .doOnNext(request -> Log.trace("Request <= {}", request))
                .observeOn(Schedulers.io())
                .subscribe(request -> {
                    final Response response = handleRequest.apply(request);
                    Log.trace("Response => {}", response);
                    adapter.write(response);
                }, throwable -> this.onError(throwable, adapter)));
    }

    private void onError(Throwable throwable, ClientSocketAdapter adapter) {
        if(adapter == null) {
            return;
        }
        try {
            adapter.close();
        } catch (IOException ignore) {
            // socket is already closed.
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
        compositeDisposable.dispose();
        compositeDisposable.clear();
    }


    protected abstract void onStart() throws IOException;
    protected abstract boolean isClosed();
    protected abstract String getProxyConnectionHint(RMIServiceInfo serviceInfo);
    protected abstract RMISocket acceptClient() throws IOException;
    protected abstract void onClose() throws IOException;

}
