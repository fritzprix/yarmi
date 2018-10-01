package com.doodream.rmovjs.sdp;

import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.serde.Converter;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private Logger Log = LoggerFactory.getLogger(SimpleServiceDiscovery.class);
    public static final int BROADCAST_PORT = 3041;
    public static final String MULTICAST_GROUP_IP = "224.0.2.118";   // AD-HOC block 1
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Override
    public synchronized void startAdvertiser(final RMIServiceInfo info, final Converter converter, boolean block) throws IOException {

        Observable<Long> tickObservable = Observable.interval(0L, 3L, TimeUnit.SECONDS);

        compositeDisposable.add(tickObservable
                .map(new Function<Long, RMIServiceInfo>() {
                    @Override
                    public RMIServiceInfo apply(Long aLong) throws Exception {
                        return info;
                    }
                })
                .map(new Function<RMIServiceInfo, DatagramPacket>() {
                    @Override
                    public DatagramPacket apply(RMIServiceInfo info) throws Exception {
                        return buildLocalPacket(info, converter);
                    }
                })
                .doOnNext(new Consumer<DatagramPacket>() {
                    @Override
                    public void accept(DatagramPacket datagramPacket) throws Exception {
                        broadcast(datagramPacket);
                    }
                })
                .doOnError(new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                    }
                })
                .subscribeOn(Schedulers.io())
                .subscribe(new Consumer<DatagramPacket>() {
                    @Override
                    public void accept(DatagramPacket datagramPacket) throws Exception {

                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        onError(throwable);
                    }
                }));

        Observable<DatagramPacket> packetObservable = tickObservable
                .map(new Function<Long, RMIServiceInfo>() {
                    @Override
                    public RMIServiceInfo apply(Long aLong) throws Exception {
                        return info;
                    }
                })
                .map(new Function<RMIServiceInfo, DatagramPacket>() {
                    @Override
                    public DatagramPacket apply(RMIServiceInfo rmiServiceInfo) throws Exception {
                        return buildLocalPacket(info, converter);
                    }
                })
                .doOnNext(new Consumer<DatagramPacket>() {
                    @Override
                    public void accept(DatagramPacket datagramPacket) throws Exception {
                        broadcast(datagramPacket);
                    }
                });

        Log.info("advertising service : {} {} @ {}", info.getName(), info.getVersion(), MULTICAST_GROUP_IP);

        if(!block) {
            compositeDisposable.add(packetObservable.subscribeOn(Schedulers.io()).subscribe(new Consumer<DatagramPacket>() {
                @Override
                public void accept(DatagramPacket datagramPacket) throws Exception {

                }
            }, new Consumer<Throwable>() {
                @Override
                public void accept(Throwable throwable) throws Exception {
                    onError(throwable);
                }
            }));
            return;
        }
        packetObservable.blockingSubscribe();
    }

    private void onError(Throwable throwable) {
        Log.error(throwable.getLocalizedMessage());
    }

    private DatagramPacket buildLocalPacket(RMIServiceInfo i, Converter converter) throws UnsupportedEncodingException, UnknownHostException {
        byte[] infoByteString = converter.convert(i);
        return new DatagramPacket(infoByteString, infoByteString.length, new InetSocketAddress(BROADCAST_PORT));
    }

    private void broadcast(DatagramPacket datagramPacket) throws IOException {
        DatagramSocket socket = new DatagramSocket();
        try {
            socket.send(datagramPacket);
        } catch (IOException e) {
            Log.warn(e.getMessage());
        } finally {
            socket.close();
        }
    }

    private DatagramPacket buildMulticastPacket(RMIServiceInfo info, Converter converter) throws UnsupportedEncodingException, UnknownHostException {
        byte[] infoByteString = converter.convert(info);
        return new DatagramPacket(infoByteString, infoByteString.length, InetAddress.getByName(MULTICAST_GROUP_IP), BROADCAST_PORT);
    }

    @Override
    public synchronized void stopAdvertiser() {
        if(!compositeDisposable.isDisposed()) {
            compositeDisposable.dispose();
        }
        compositeDisposable.clear();
    }

}
