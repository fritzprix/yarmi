package com.doodream.rmovjs.sdp;

import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.serde.Converter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 *  this class is counter implementation of SimpleServiceAdvertiser, which is intended to be used for testing purpose as well
 *
 *  listen broadcast message and try to convert the message into RMIServiceInfo.
 *  if there is service broadcast matched to the target service info, then invoke onDiscovered callback of listener
 */
public class SimpleServiceDiscovery extends BaseServiceDiscovery {

    private static Logger Log = LogManager.getLogger(SimpleServiceDiscovery.class);
    private DatagramSocket serviceBroadcastSocket;

    public SimpleServiceDiscovery() throws SocketException {
        super(100L, TimeUnit.MILLISECONDS);
        serviceBroadcastSocket = new DatagramSocket(new InetSocketAddress(SimpleServiceAdvertiser.BROADCAST_PORT));
    }

    @Override
    protected RMIServiceInfo recvServiceInfo(Converter converter) throws IOException {
        Log.debug(converter);
        byte[] buffer = new byte[64 * 1024];
        Arrays.fill(buffer, (byte) 0);
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        serviceBroadcastSocket.receive(packet);
        RMIServiceInfo info = converter.invert(packet.getData(), RMIServiceInfo.class);
        Log.debug(info);
        return info;
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
