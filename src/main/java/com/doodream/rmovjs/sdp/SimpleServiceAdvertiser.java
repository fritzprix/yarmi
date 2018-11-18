package com.doodream.rmovjs.sdp;

import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.serde.Converter;
import com.doodream.rmovjs.serde.json.JsonConverter;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.*;
import io.reactivex.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * this class provides simple service advertising capability whose intended use is testing though,
 * can be used a simple service discovery scenario.
 *
 * advertiser start to advertise its RMIServiceInfo with broadcasting datagram socket
 */
public class SimpleServiceAdvertiser implements ServiceAdvertiser {

    private Logger Log = LoggerFactory.getLogger(SimpleServiceDiscovery.class);
    public static final int BROADCAST_PORT = 3041;
    public static final String MULTICAST_GROUP_IP = "224.0.2.118";   // AD-HOC block 1
    private CompositeDisposable compositeDisposable = new CompositeDisposable();


    public static InetAddress getGroupAddress() throws UnknownHostException {
        return InetAddress.getByName(MULTICAST_GROUP_IP);
    }

    @Override
    public synchronized void startAdvertiser(final RMIServiceInfo info, boolean block) throws IOException {

        final List<MulticastSocket> socketList = new ArrayList<>();
        final Observable<Long> tickObservable = Observable.interval(0L, 3L, TimeUnit.SECONDS);
        final JsonConverter converter = new JsonConverter();

        List<NetworkInterface> networks = Collections.list(NetworkInterface.getNetworkInterfaces());
        for (NetworkInterface network : networks) {
            if (!network.isUp()) {
                continue;
            }
            findValidInterface(network.getInterfaceAddresses())
                    .map(new Function<InetAddress, MulticastSocket>() {
                        @Override
                        public MulticastSocket apply(InetAddress ifc) throws Exception {
                            MulticastSocket socket = new MulticastSocket(BROADCAST_PORT);
                            socket.setInterface(ifc);
                            socket.setTimeToLive(2);
                            return socket;
                        }
                    })
                    .blockingSubscribe(new Consumer<MulticastSocket>() {
                        @Override
                        public void accept(MulticastSocket socket) throws Exception {
                            socketList.add(socket);
                        }
                    });
        }

        Observable<DatagramPacket> packetObservable = tickObservable
                .map(new Function<Long, RMIServiceInfo>() {
                    @Override
                    public RMIServiceInfo apply(Long aLong) throws Exception {
                        return info;
                    }
                })
                .map(new Function<RMIServiceInfo, DatagramPacket>() {
                    @Override
                    public DatagramPacket apply(RMIServiceInfo info) throws Exception {
                        return buildMulticastPacket(info, converter);
                    }
                })
                .doOnNext(new Consumer<DatagramPacket>() {
                    @Override
                    public void accept(DatagramPacket datagramPacket) throws Exception {
                        for (MulticastSocket socket : socketList) {
                            Log.debug("send Service Info @ {}", socket.getInterface());
                            socket.send(datagramPacket);
                        }
                    }
                })
                .doOnDispose(new Action() {
                    @Override
                    public void run() throws Exception {
                        for (MulticastSocket socket : socketList) {
                            if(!socket.isClosed()) {
                                socket.close();
                            }
                        }
                    }
                })
                .doOnError(new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        onError(throwable);
                    }
                })
                .subscribeOn(Schedulers.io());
        if(!block) {
            compositeDisposable.add(packetObservable.subscribe());
        } else {
            packetObservable.blockingSubscribe();
        }
    }

    private Observable<InetAddress> findValidInterface(List<InterfaceAddress> interfaceAddresses) {
        return Observable.fromIterable(interfaceAddresses)
                .map(new Function<InterfaceAddress, InetAddress>() {
                    @Override
                    public InetAddress apply(InterfaceAddress interfaceAddress) throws Exception {
                        return interfaceAddress.getAddress();
                    }
                });
    }

    @Override
    public void startAdvertiser(RMIServiceInfo info, boolean block, InetAddress inf) throws IOException {
        final Observable<Long> tickObservable = Observable.interval(0L, 3L, TimeUnit.SECONDS);
        final JsonConverter converter = new JsonConverter();
        final MulticastSocket socket = new MulticastSocket(BROADCAST_PORT);


        Observable<DatagramPacket> packetObservable = tickObservable
                .map(new Function<Long, RMIServiceInfo>() {
                    @Override
                    public RMIServiceInfo apply(Long aLong) throws Exception {
                        return info;
                    }
                })
                .map(new Function<RMIServiceInfo, DatagramPacket>() {
                    @Override
                    public DatagramPacket apply(RMIServiceInfo info) throws Exception {
                        return buildMulticastPacket(info, converter);
                    }
                })
                .doOnNext(new Consumer<DatagramPacket>() {
                    @Override
                    public void accept(DatagramPacket datagramPacket) throws Exception {
                        Log.debug("send Service Info @ {}", socket.getInterface());
                        socket.send(datagramPacket);
                    }
                })
                .doOnDispose(new Action() {
                    @Override
                    public void run() throws Exception {
                        if(!socket.isClosed()) {
                            socket.close();
                        }
                    }
                })
                .doOnError(new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        onError(throwable);
                    }
                })
                .subscribeOn(Schedulers.io());
        if(!block) {
            compositeDisposable.add(packetObservable.subscribe());
        } else {
            packetObservable.blockingSubscribe();
        }
    }

    private void onError(Throwable throwable) {
        Log.error(throwable.getLocalizedMessage());
    }

    private DatagramPacket buildLocalPacket(RMIServiceInfo i, Converter converter) throws UnsupportedEncodingException, UnknownHostException {
        byte[] infoByteString = converter.convert(i);
        return new DatagramPacket(infoByteString, infoByteString.length, new InetSocketAddress(BROADCAST_PORT));
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
