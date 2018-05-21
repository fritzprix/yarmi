package com.doodream.rmovjs.sdp;

import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.net.ServiceAdapter;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

/**
 *  this class is counter implementation of SimpleServiceAdvertiser, which is intended to be used for testing purpose as well
 *
 *  listen broadcast message and try to convert the message into RMIServiceInfo.
 *  if there is service broadcast matched to the target service info, then invoke onDiscovered callback of listener
 */
public class SimpleServiceDiscovery implements ServiceDiscovery {
    private HashMap<RMIServiceInfo, Disposable> disposableHashMap = new HashMap<>();
    private HashSet<Integer> discovered = new HashSet<>();
    private static final byte[] EMPTY_DATA = new byte[0];


    private DatagramPacket receivePacket(DatagramSocket datagramSocket) throws IOException {
        byte[] buffer = new byte[64 * 1024];
        Arrays.fill(buffer, (byte) 0);
        DatagramPacket packet = new DatagramPacket(buffer,buffer.length);
        try {
            datagramSocket.receive(packet);
        } catch (Exception e ) {
            packet.setData(EMPTY_DATA);
            return packet;
        }
        return packet;
    }


    private Observable<Long> discoveryTickEventObservable() {
        return Observable.interval(0L, 100L, TimeUnit.MILLISECONDS);
    }

    @Override
    public void startDiscovery(RMIServiceInfo info, ServiceDiscoveryListener listener, long timeout, TimeUnit unit) throws IOException {
        DatagramSocket socket = new DatagramSocket(new InetSocketAddress(SimpleServiceAdvertiser.BROADCAST_PORT));
        discovered.clear();
        Observable<Long> tickObservable = discoveryTickEventObservable();
        Observable<RMIServiceInfo> serviceInfoObservable = tickObservable
                .map(aLong -> receivePacket(socket))
                .map(DatagramPacket::getData)
                .filter(bytes -> bytes.length > 0)
                .map(RMIServiceInfo::from)
                .filter(service -> discovered.add(service.hashCode()))
                .filter(info::equals)
                .timeout(timeout, unit);

        disposableHashMap.put(info,serviceInfoObservable
                .map(RMIServiceInfo::getAdapter)
                .map(Class::newInstance)
                .cast(ServiceAdapter.class)
                .map(adapter->adapter.getProxyFactory(info))
                .doOnDispose(socket::close)
                .map(serviceProxyFactory -> serviceProxyFactory.build(info))
                .doOnDispose(() -> {
                    listener.onDiscoveryFinished();
                    onStopDiscovery(info, socket);
                })
                .doOnError(throwable -> {
                    listener.onDiscoveryFinished();
                    onStopDiscovery(info, socket);
                })
                .doOnComplete(() -> {
                    listener.onDiscoveryFinished();
                    onStopDiscovery(info, socket);
                })
                .subscribe(listener::onDiscovered));

        listener.onDiscoveryStarted();

    }

    private void onStopDiscovery(RMIServiceInfo info, DatagramSocket socket) {
        Disposable disposable = disposableHashMap.get(info);
        if(disposable != null &&
                !disposable.isDisposed()) {
            disposable.dispose();
        }
        if(socket.isClosed()) {
            return;
        }
        socket.close();
    }


    @Override
    public void startDiscovery(RMIServiceInfo info, ServiceDiscoveryListener listener) throws IOException {
        startDiscovery(info, listener, 5L, TimeUnit.SECONDS);
    }

    @Override
    public void cancelDiscovery(RMIServiceInfo info) {
        Disposable disposable = disposableHashMap.get(info);
        if(disposable == null) {
            return;
        }
        if(disposable.isDisposed()) {
            return;
        }
        disposable.dispose();
    }

}
