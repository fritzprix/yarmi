package com.doodream.rmovjs.net;


import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.model.Request;
import com.doodream.rmovjs.model.Response;
import com.doodream.rmovjs.serde.Converter;
import io.reactivex.functions.Function;

import java.io.IOException;
import java.net.NetworkInterface;

/**
 *  {@link ServiceAdapter} provides abstraction layer for network dependency including listed below
 *  1. creating server side endpoint to communicate with client (e.g. {@link java.net.ServerSocket} in case of TCP connection)
 *  2. accepting connection request from the client & managing {@link ClientSocketAdapter} which provides abstraction to the client connection from server side
 *  3. providing {@link ServiceProxyFactory} able to resolve {@link RMIServiceProxy}, which is network peer corresponding to {@link ServiceAdapter}
 *  4. handling negotiation protocol between the server and the client
 *
 *  {@link ServiceAdapter}는 서비스의 네트워크 의존성에 대한 추상화 계층을 제공하며 세부적으로 아래와 같은 기능을 포함한다.
 *  1. server와 client간의 통신을 위한 서버측 네트워크 endpoint의 생성 (creating {@link java.net.ServerSocket} and bind it to given address for TCP)
 *  2. client 연결 요청에 대한 처리와 client 네트워크 연결을 서버측에서 추상화하는 {@link ClientSocketAdapter}의 관리
 *  3. client측의 service 기능 호출의 proxy역할을 담당하는 {@link RMIServiceProxy} 생성을 위한 {@link ServiceProxyFactory}의 제공
 *  4. client와 server 사이의 negotiation protocol에 대한 처리 (단, 상세 negotiation 구현은 {@link RMIServiceInfo}의 {@link RMINegotiator}로써 제공됨)
 */
public interface ServiceAdapter {

    /**
     *
     * client로 부터의 네트워크 연결을 대기하며 client로 부터의 {@link Request}를 처리하기 위한 handler를 등록한다.
     * @param serviceInfo 서비스 정의 instance {@link RMIServiceInfo}
     * @param converter {@link Request} 및 {@link Response} instance에 대한 deserialization / serialization module
     * @param network network interface which the service adapter listen to
     * @param requestHandler {@link Request}의 수신 및 {@link Response}의 응답을 처리하기 위한 handler로 {@link com.doodream.rmovjs.server.RMIService}
     * @return proxyFactoryHint as string
     * @throws IOException server 측 네트워크 endpoint 생성의 실패 혹은 I/O 오류
     * @throws IllegalAccessError the error thrown when {@link ServiceAdapter} fails to resolve dependency object (e.g. negotiator,
     * @throws InstantiationException if dependent class represents an abstract class,an interface, an array class, a primitive type, or void;or if the class has no nullary constructor;
     */
    String listen(RMIServiceInfo serviceInfo, Converter converter, NetworkInterface network, Function<Request, Response> requestHandler) throws IOException, IllegalAccessException, InstantiationException;

    /**
     * return {@link ServiceProxyFactory} which is capable of building {@link RMIServiceProxy} able to connect to current service adapter
     * 현재 {@link ServiceAdapter}에 대응 되는 client측 peer를 생성 할 수 있는 {@link ServiceProxyFactory}를 반환
     * @param info 서비스 정의 instance
     * @return 현재 {@link ServiceAdapter}와 연결이 가능한 Peer {@link RMIServiceProxy}의 {@link ServiceProxyFactory}
     */
    ServiceProxyFactory getProxyFactory(RMIServiceInfo info);

    /**
     * server 측 network 연결을 해제하고 모든 resource를 반환
     */
    void close();
}
