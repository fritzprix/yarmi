package com.doodream.rmovjs.sdp;

import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.serde.Converter;
import com.doodream.rmovjs.serde.json.JsonConverter;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

/**
 *  this class is counter implementation of SimpleServiceAdvertiser, which is intended to be used for testing purpose as well
 *
 *  listen broadcast message and try to convert the message into RMIServiceInfo.
 *  if there is service broadcast matched to the target service info, then invoke onDiscovered callback of listener
 */
public class SimpleServiceDiscovery extends BaseServiceDiscovery {

    private DiscoveryEventListener listener;
    private final CompositeDisposable disposable;
    private final MulticastSocket serviceBroadcastSocket;
    private final JsonConverter converter;

    public SimpleServiceDiscovery() throws IOException {
        super();
        disposable = new CompositeDisposable();
        converter = new JsonConverter();
        serviceBroadcastSocket = new MulticastSocket(SimpleServiceAdvertiser.BROADCAST_PORT);
        serviceBroadcastSocket.joinGroup(InetAddress.getByName(SimpleServiceAdvertiser.MULTICAST_GROUP_IP));
    }

    @Override
    protected void onStartDiscovery(DiscoveryEventListener listener) {
        this.listener = listener;
        disposable.add(Observable.interval(1000L, TimeUnit.MILLISECONDS)
                .map(new Function<Long, RMIServiceInfo>() {
                    @Override
                    public RMIServiceInfo apply(Long aLong) throws Exception {
                        byte[] buffer = new byte[64 * 1024];
                        Arrays.fill(buffer, (byte) 0);
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        serviceBroadcastSocket.receive(packet);
                        return converter.invert(packet.getData(), RMIServiceInfo.class);
                    }
                })
                .subscribe(new Consumer<RMIServiceInfo>() {
                    @Override
                    public void accept(RMIServiceInfo info) throws Exception {
                        listener.onDiscovered(info);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        listener.onError(throwable);
                    }
                }, new Action() {
                    @Override
                    public void run() throws Exception {
                        listener.onStop();
                    }
                }));
    }

    @Override
    protected void close() {
        if(!disposable.isDisposed()) {
            disposable.dispose();
        }
        disposable.clear();
        if(!serviceBroadcastSocket.isClosed()) {
            serviceBroadcastSocket.close();
        }
    }
}
