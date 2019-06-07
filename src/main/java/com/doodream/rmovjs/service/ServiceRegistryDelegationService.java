package com.doodream.rmovjs.service;

import com.doodream.rmovjs.annotation.parameter.AdapterParam;
import com.doodream.rmovjs.annotation.server.Controller;
import com.doodream.rmovjs.annotation.server.Service;
import com.doodream.rmovjs.model.RMIError;
import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.model.Response;
import com.doodream.rmovjs.net.tcp.TcpServiceAdapter;

@Service(name = "sdp.registry", provider = "com.doodream", params = {
        @AdapterParam(key = TcpServiceAdapter.PARAM_PORT,value = "3043")
})
public class ServiceRegistryDelegationService {

    public static class DefaultRegistryController implements ServiceRegistryController {


        @Override
        public Response<Integer> register(RMIServiceInfo service) {
            return RMIError.NOT_IMPLEMENTED.getResponse();
        }

        @Override
        public Response unregister(int id) {
            return RMIError.NOT_IMPLEMENTED.getResponse();
        }
    }

    @Controller(path = "/registry", version = 1, module = DefaultRegistryController.class)
    ServiceRegistryController registryController;
}
