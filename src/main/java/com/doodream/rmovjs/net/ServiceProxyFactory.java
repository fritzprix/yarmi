package com.doodream.rmovjs.net;

import com.doodream.rmovjs.model.RMIServiceInfo;

import java.io.IOException;

/**
 *  ServiceInfo를 기반으로 클라이언트측의 서비스 정의 객체를 생성 한다.
 *  이 클래스는 peer간의 하부네트워크 계층에 따라 정의 되어야하나
 *  공통적으로 실제 peer간의 연결 자체와 handshake등의 정책은 처리하지 않는다.
 *  단,연결 및 정책 정보를 연결 이전의 proxy instance에 주입 시키는 동작을 수행한다.
 *
 *  ServiceProxyFactory는 ServiceAdapter에 대한 client측의 추상화 계층을 담당하는
 *  ServiceProxy인스턴스를 반복적으로 생성하는 역알을 수행하기 때문에 ServiceInfo에는
 *  ServiceAdapter의 구상클래스 정보와 이에 대한 생성자 parameter정보가 전달 된다.
 *  이를 통해서 ServiceProxyFactory는 현재 서버측의 네트워킹을 위한 파라미터들을 유추 혹은 추출 할 수 있다.
 */
public interface ServiceProxyFactory {
    /**
     * build RMIServiceProxy to remote service adapter would be comprised of below
     * 1. create RMISocket on which RMIServiceProxy depend
     * @return
     * @throws IOException
     */
    RMIServiceProxy build() throws IOException;
    void setTargetService(RMIServiceInfo info);
}
