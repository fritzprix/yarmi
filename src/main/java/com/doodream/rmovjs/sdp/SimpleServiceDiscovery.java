package com.doodream.rmovjs.sdp;

import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.net.RMIServiceProxy;
import com.doodream.rmovjs.net.ServiceAdapter;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

import java.io.IOException;
import java.net.*;
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
public class SimpleServiceDiscovery extends BaseServiceDiscovery {

    private DatagramSocket serviceBroadcastSocket;

    public SimpleServiceDiscovery() throws SocketException {
        super(100L, TimeUnit.MILLISECONDS);
        serviceBroadcastSocket = new DatagramSocket(new InetSocketAddress(SimpleServiceAdvertiser.BROADCAST_PORT));
    }

    @Override
    protected RMIServiceInfo recvServiceInfo() throws IOException {
        byte[] buffer = new byte[64 * 1024];
        Arrays.fill(buffer, (byte) 0);
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        serviceBroadcastSocket.receive(packet);
        return RMIServiceInfo.from(packet.getData());
    }

    @Override
    protected void close() {
        if(serviceBroadcastSocket == null) {
            return;
        }
        if(serviceBroadcastSocket.isClosed()) {
            return;
        }
        serviceBroadcastSocket.close();
    }
}
