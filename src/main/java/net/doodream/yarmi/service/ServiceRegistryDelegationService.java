package net.doodream.yarmi.service;

import net.doodream.yarmi.annotation.parameter.AdapterParam;
import net.doodream.yarmi.annotation.server.Controller;
import net.doodream.yarmi.annotation.server.Service;
import net.doodream.yarmi.model.RMIError;
import net.doodream.yarmi.model.RMIServiceInfo;
import net.doodream.yarmi.model.Response;
import net.doodream.yarmi.net.tcp.TcpServiceAdapter;
import net.doodream.yarmi.serde.bson.BsonConverter;

@Service(name = "sdp.registry", provider = "com.doodream", params = {
        @AdapterParam(key = TcpServiceAdapter.PARAM_PORT,value = "3043")
}, converter = BsonConverter.class)
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
