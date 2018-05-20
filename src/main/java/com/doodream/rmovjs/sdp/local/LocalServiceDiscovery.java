package com.doodream.rmovjs.sdp.local;

import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.net.ServiceAdapter;
import com.doodream.rmovjs.sdp.ServiceDiscovery;
import com.doodream.rmovjs.sdp.ServiceDiscoveryListener;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

public class LocalServiceDiscovery implements ServiceDiscovery {
    private HashMap<RMIServiceInfo, Disposable> disposableHashMap = new HashMap<>();
    private HashSet<Integer> discovered;


    private DatagramPacket receivePacket(DatagramSocket datagramSocket) throws IOException {
        byte[] buffer = new byte[64 * 64 * 1024];
        Arrays.fill(buffer, (byte) 0);
        DatagramPacket packet = new DatagramPacket(buffer,buffer.length);
        datagramSocket.receive(packet);
        return packet;
    }



    @Override
    public void discover(RMIServiceInfo info, ServiceDiscoveryListener listener, long timeout, TimeUnit unit) throws IOException {
        DatagramSocket socket = new DatagramSocket(new InetSocketAddress(LocalServiceAdvertiser.BROADCAST_PORT));
        Observable<Long> tickObservable = Observable.interval(0L, 1L, TimeUnit.SECONDS);
        discovered = new HashSet<>();


        // listen broadcast message from datagram socket
        // and convert it to RMIServiceInfo
        Observable<RMIServiceInfo> serviceInfoObservable = tickObservable
                .scan((aLong, aLong2) -> aLong + aLong2)
                .filter(aLong -> aLong < timeout)
                .map(along-> receivePacket(socket))
                // socket error
                .map(DatagramPacket::getData)
                .map(RMIServiceInfo::from)
                .filter(advInfo -> discovered.add(advInfo.hashCode()))
                // format error
                .filter(discovered -> discovered.equals(info));

        disposableHashMap.put(info,serviceInfoObservable
                .map(RMIServiceInfo::getAdapter)
                .map(Class::newInstance)
                .cast(ServiceAdapter.class)
                .map(adapter->adapter.getProxyFactory(info))
                .doOnDispose(socket::close)
                .map(serviceProxyFactory -> serviceProxyFactory.build(info))
                .subscribe(listener::onDiscovered, throwable -> {
                    throwable.printStackTrace();
                    socket.close();
                }, socket::close));
    }

    @Override
    public void startDiscovery(RMIServiceInfo info, ServiceDiscoveryListener listener) throws IOException {
        DatagramSocket socket = new DatagramSocket(new InetSocketAddress(LocalServiceAdvertiser.BROADCAST_PORT));
        Observable<Long> tickObservable = Observable.interval(0L, 10L, TimeUnit.SECONDS);
        Disposable disposable = disposableHashMap.get(info);
        if(disposable != null) {
            return;
        }

        // listen broadcast message from datagram socket
        // and convert it to RMIServiceInfo
        Observable<RMIServiceInfo> serviceInfoObservable = tickObservable
                .scan((aLong, aLong2) -> aLong + aLong2)
                .map(along-> receivePacket(socket))
                // socket error
                .map(DatagramPacket::getData)
                .map(RMIServiceInfo::from)
                // format error
                .filter(discovered -> discovered.equals(info));

        disposableHashMap.put(info, serviceInfoObservable
                .map(RMIServiceInfo::getAdapter)
                .map(Class::newInstance)
                .cast(ServiceAdapter.class)
                .map(adapter->adapter.getProxyFactory(info))
                .doOnDispose(socket::close)
                .map(serviceProxyFactory -> serviceProxyFactory.build(info))
                .subscribeOn(Schedulers.io())
                .subscribe(listener::onDiscovered, throwable -> {
                    throwable.printStackTrace();
                    socket.close();
                }, socket::close));
    }

    @Override
    public void stopDiscovery(RMIServiceInfo info) {
        Disposable disposable = disposableHashMap.get(info);
        discovered.clear();
        if(disposable == null) {
            return;
        }
        if(disposable.isDisposed()) {
            return;
        }
        disposable.dispose();
    }

}
