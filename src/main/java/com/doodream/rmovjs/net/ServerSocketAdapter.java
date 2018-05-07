package com.doodream.rmovjs.net;

import com.doodream.rmovjs.model.Endpoint;
import com.doodream.rmovjs.model.Request;
import com.doodream.rmovjs.model.Response;
import com.doodream.rmovjs.model.ServiceInfo;

public interface ServerSocketAdapter {
    Response request(ServiceInfo info, Endpoint endpoint);
    Response request(Request request);
}
