package com.doodream.rmovjs.sdp;

import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.serde.Converter;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.concurrent.TimeUnit;

/**
 * this class provices simple service advertising capability whose intented use is testing though,
 * can be used a simple service discovery scenario.
 *
 * advertiser start to advertise its RMIServiceInfo with broadcasting datagram socket
 */
public class SimpleServiceAdvertiser implements ServiceAdvertiser {

    private Logger Log = LogManager.getLogger(SimpleServiceDiscovery.class);
    public static final int BROADCAST_PORT = 3041;
    public static final String MULTICAST_GROUP_IP = "224.0.2.118";   // AD-HOC block 1
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Override
    public synchronized void startAdvertiser(RMIServiceInfo info, Converter converter, boolean block) throws IOException {

        Observable<Long> tickObservable = Observable.interval(0L, 3L, TimeUnit.SECONDS);

        compositeDisposable.add(tickObservable
                .map(aLong -> info)
                .map(i -> buildLocalPacket(i, converter))
                .doOnNext(this::broadcast)
                .doOnError(Throwable::printStackTrace)
                .subscribeOn(Schedulers.io())
                .subscribe());

        Observable<DatagramPacket> packetObservable = tickObservable
                .map(aLong -> info)
                .map(i -> buildMulticastPacket(i, converter))
                .doOnNext(this::broadcast)
                .doOnError(Throwable::printStackTrace);

        if(!block) {
            compositeDisposable.add(packetObservable.subscribeOn(Schedulers.io()).subscribe());
            return;
        }
        packetObservable.blockingSubscribe();
    }

    private DatagramPacket buildLocalPacket(RMIServiceInfo i, Converter converter) throws UnsupportedEncodingException, UnknownHostException {
        byte[] infoByteString = converter.convert(i);
        return new DatagramPacket(infoByteString, infoByteString.length, new InetSocketAddress(BROADCAST_PORT));
    }

    private void broadcast(DatagramPacket datagramPacket) throws IOException {
        DatagramSocket socket = new DatagramSocket();
//        socket.setBroadcast(true);
        socket.send(datagramPacket);
        socket.close();
    }

    private DatagramPacket buildMulticastPacket(RMIServiceInfo info, Converter converter) throws UnsupportedEncodingException, UnknownHostException {
        byte[] infoByteString = converter.convert(info);
        return new DatagramPacket(infoByteString, infoByteString.length, InetAddress.getByName(MULTICAST_GROUP_IP), BROADCAST_PORT);
//        return new DatagramPacket(infoByteString, infoByteString.length, new InetSocketAddress(BROADCAST_PORT));
    }

    @Override
    public synchronized void stopAdvertiser() {
        if(!compositeDisposable.isDisposed()) {
            compositeDisposable.dispose();
        }
        compositeDisposable.clear();
    }

}
