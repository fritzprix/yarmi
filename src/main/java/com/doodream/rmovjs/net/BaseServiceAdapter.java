package com.doodream.rmovjs.net;

import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.model.Request;
import com.doodream.rmovjs.model.Response;
import com.doodream.rmovjs.serde.Converter;
import com.google.common.base.Preconditions;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import lombok.NonNull;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public abstract class BaseServiceAdapter implements ServiceAdapter {

    protected static final Logger Log = LogManager.getLogger(BaseServiceAdapter.class);
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private volatile boolean listen = false;

    @Override
    public String listen(RMIServiceInfo serviceInfo, Converter converter, @NonNull Function<Request, Response> handleRequest) throws IllegalAccessException, InstantiationException, IOException {
        if(listen) {
            throw new IllegalStateException("service already listening");
        }
        final RMINegotiator negotiator = (RMINegotiator) serviceInfo.getNegotiator().newInstance();
        Preconditions.checkNotNull(negotiator, "fail to instantiate %s", serviceInfo.getNegotiator());
        Preconditions.checkNotNull(converter, "fail to instantiate %s", serviceInfo.getConverter());
        onStart();

        listen = true;
        compositeDisposable.add(Observable.just(converter)
                .map(c -> acceptClient())
                .repeatUntil(() -> !listen)
                .map(client -> negotiator.handshake(client, serviceInfo, converter, false))
                .map(socket -> new ClientSocketAdapter(socket, converter))
                .subscribeOn(Schedulers.io())
                .subscribe(adapter-> onHandshakeSuccess(adapter, handleRequest),this::onError));
        return getProxyConnectionHint(serviceInfo);
    }

    private void onHandshakeSuccess(ClientSocketAdapter adapter, Function<Request, Response> handleRequest) throws IOException {
        compositeDisposable.add(adapter
                .listen()
                .doOnNext(request -> request.setClient(adapter))
                .doOnNext(request -> Log.info("Server <= {}", request))
                .filter(Request::isValid)
                .map(handleRequest)
                .doOnError(this::onError)
                .doOnNext(adapter::write)
                .subscribe());
    }


    private void onError(Throwable throwable) {
        Log.error(throwable);
        close();
    }


    @Override
    public void close() {
        listen = false;
        if(!isClosed()) {
            try {
                onClose();
            } catch (IOException e) {
                Log.warn(e);
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
