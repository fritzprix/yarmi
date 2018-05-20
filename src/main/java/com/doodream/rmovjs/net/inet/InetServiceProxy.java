package com.doodream.rmovjs.net.inet;

import com.doodream.rmovjs.model.Endpoint;
import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.model.Request;
import com.doodream.rmovjs.model.Response;
import com.doodream.rmovjs.net.HandshakeFailException;
import com.doodream.rmovjs.net.RMIServiceProxy;
import com.doodream.rmovjs.net.RMISocket;
import lombok.Data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;


@Data
public class InetServiceProxy implements RMIServiceProxy {

    private RMIServiceInfo serviceInfo;
    private RMISocket socket;
    private BufferedReader reader;
    private PrintStream writer;


    public static InetServiceProxy create(RMIServiceInfo info, RMISocket socket) throws IOException {
        return new InetServiceProxy(info, socket);
    }

    private InetServiceProxy(RMIServiceInfo info, RMISocket socket) throws IOException {
        serviceInfo = info;
        this.socket = socket;
    }

    @Override
    public void open() throws IOException {
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        write(this.serviceInfo.toJson());
        Response response = Response.fromJson(reader.readLine());
        if(response.isSuccessful()) {
            return;
        }
        throw new HandshakeFailException(null);
    }

    private void write(String s) throws IOException {
        socket.getOutputStream().write(s.concat("\n").getBytes());
    }

    @Override
    public Response request(Endpoint endpoint) throws IOException {

        Request request = Request.builder()
                        .endpoint(endpoint)
                        .serviceInfo(serviceInfo)
                        .build();

        write(Request.toJson(request));
        return Response.fromJson(reader.readLine());
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
