package com.doodream.rmovjs.net.inet;

import com.doodream.rmovjs.model.Endpoint;
import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.model.Request;
import com.doodream.rmovjs.model.Response;
import com.doodream.rmovjs.net.RMIServiceProxy;
import com.doodream.rmovjs.net.RMISocket;
import lombok.Data;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Scanner;

@Data
public class InetServiceProxy implements RMIServiceProxy {

    private RMIServiceInfo serviceInfo;
    private RMISocket socket;
    private Scanner reader;
    private PrintStream writer;


    public static InetServiceProxy create(RMIServiceInfo info, RMISocket socket) throws IOException {
        return new InetServiceProxy(info, socket);
    }

    private InetServiceProxy(RMIServiceInfo info, RMISocket socket) {
        serviceInfo = info;
        this.socket = socket;
    }

    @Override
    public void open() throws IOException {
        writer = new PrintStream(socket.getOutputStream());
        reader = new Scanner(socket.getInputStream());

        // read out service Info from server
        String serviceInfo = reader.nextLine();
        System.out.println(serviceInfo);
        writer.println(this.serviceInfo.toJson());
        Response response = Response.fromJson(reader.nextLine());
        System.out.println(response);
    }

    @Override
    public Response request(Endpoint endpoint) throws IOException {


        writer.println(Request.toJson(Request.builder()
                .endpoint(endpoint)
                .serviceInfo(serviceInfo)
                .build()));

        return Response.fromJson(reader.nextLine());
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
