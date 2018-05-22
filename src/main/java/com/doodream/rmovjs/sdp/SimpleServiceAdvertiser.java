package com.doodream.rmovjs.sdp;

import com.doodream.rmovjs.model.RMIServiceInfo;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * this class provices simple service advertising capability whose intented use is testing though,
 * can be used a simple service discovery scenario.
 *
 * advertiser start to advertise its RMIServiceInfo with broadcasting datagram socket
 */
public class SimpleServiceAdvertiser implements ServiceAdvertiser {

    public static final int BROADCAST_PORT = 3041;
    private Disposable disposable;


    @Override
    public synchronized void startAdvertiser(RMIServiceInfo info, boolean block) {

        Observable<DatagramPacket> packetObservable = Observable.interval(0L, 3L, TimeUnit.SECONDS)
                .map(aLong -> info)
                .map(this::buildBroadcastPackaet)
                .doOnNext(this::broadcast)
                .doOnError(Throwable::printStackTrace);
        if(!block) {
            disposable = packetObservable.subscribeOn(Schedulers.io()).subscribe();
            return;
        }
        packetObservable.blockingSubscribe();
    }

    private void broadcast(DatagramPacket datagramPacket) throws IOException {
        DatagramSocket socket = new DatagramSocket();
        socket.setBroadcast(true);
        socket.send(datagramPacket);
    }

    private DatagramPacket buildBroadcastPackaet(RMIServiceInfo info) throws UnsupportedEncodingException {
        byte[] infoByteString = info.toJson().getBytes("UTF-8");
        return new DatagramPacket(infoByteString, infoByteString.length, new InetSocketAddress(BROADCAST_PORT));
    }

    @Override
    public synchronized void stopAdvertiser() throws IOException {
        if(disposable == null) {
            return;
        }
        disposable.dispose();
    }
}
