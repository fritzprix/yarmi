package com.doodream.rmovjs.sdp;

import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.serde.json.JsonConverter;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 *  this class is counter implementation of SimpleServiceAdvertiser, which is intended to be used for testing purpose as well
 *
 *  listen broadcast message and try to convert the message into RMIServiceInfo.
 *  if there is service broadcast matched to the target service info, then invoke onDiscovered callback of listener
 */
public class SimpleServiceDiscovery extends BaseServiceDiscovery {


    private static final Logger Log = LoggerFactory.getLogger(SimpleServiceDiscovery.class);
    private final CompositeDisposable disposable;
    private final JsonConverter converter;

    public SimpleServiceDiscovery() {
        super();
        disposable = new CompositeDisposable();
        converter = new JsonConverter();

    }

    @Override
    protected void onStartDiscovery(DiscoveryEventListener listener) {
        try {
            Enumeration<NetworkInterface> networkInterfaceEnumeration = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaceEnumeration.hasMoreElements()) {
                final NetworkInterface network = networkInterfaceEnumeration.nextElement();
                if (!network.isUp()) {
                    continue;
                }
                for (InterfaceAddress interfaceAddress : network.getInterfaceAddresses()) {
                    Log.debug("subscribe SDP on {}", interfaceAddress.getAddress());
                    final MulticastSocket socket = new MulticastSocket(SimpleServiceAdvertiser.BROADCAST_PORT);
                    socket.setInterface(interfaceAddress.getAddress());
                    socket.joinGroup(SimpleServiceAdvertiser.getGroupAddress());
                    disposable.add(listenMulticast(socket, 1000L, TimeUnit.MILLISECONDS)
                            .map(new Function<byte[], RMIServiceInfo>() {
                                @Override
                                public RMIServiceInfo apply(byte[] bytes) throws Exception {
                                    return converter.invert(bytes, RMIServiceInfo.class);
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
                            .subscribeOn(Schedulers.io())
                            .subscribe(new Consumer<RMIServiceInfo>() {
                                @Override
                                public void accept(RMIServiceInfo info) throws Exception {
                                    Log.debug("service discovered ({}) on {}", info.getName(), socket.getInterface());
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
            }
        } catch (IOException e) {
            Log.error("fail to start discovery", e);
        }
    }

    private Observable<byte[]> listenMulticast(MulticastSocket socket, long interval, TimeUnit timeUnit) throws IOException {
        return Observable.interval(interval, timeUnit)
                .map(new Function<Long, DatagramPacket>() {
                    @Override
                    public DatagramPacket apply(Long aLong) throws Exception {
                        byte[] buffer = new byte[64 * 1024];
                        Arrays.fill(buffer, (byte) 0);
                        return new DatagramPacket(buffer, buffer.length);
                    }
                })
                .map(new Function<DatagramPacket, byte[]>() {
                    @Override
                    public byte[] apply(DatagramPacket datagramPacket) throws Exception {
                        socket.receive(datagramPacket);
                        return datagramPacket.getData();
                    }
                });
    }

    @Override
    protected void onStopDiscovery() {
        if(!disposable.isDisposed()) {
            disposable.dispose();
        }
        disposable.clear();
    }
}
