package com.doodream.rmovjs.net;

import com.doodream.rmovjs.model.Endpoint;
import com.doodream.rmovjs.model.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * pooled service proxy that keeps track of multiple service proxies and prioritize proxies along the given policy
 */
public class ManagedPoolServiceProxy implements ServiceProxy {
    private static final Logger Log = LoggerFactory.getLogger(ManagedPoolServiceProxy.class);
    // endpoint for health check

    public enum PrioritizePolicy {
        LOWEST_LATECNY_FIRST,
        LEAST_LOAD_FIRST,
        ROUND_ROBIN
    }

    public enum DropPolicy {
        NEVER,
        CONNECTION_LOST,
        TIMEOUT
    }

    public static class ServicePoolingPolicy {

        public static class Builder {
            ServicePoolingPolicy policy;
            public Builder() {
                policy = new ServicePoolingPolicy();
            }

            public Builder setPrioritizePolicy(PrioritizePolicy prioritizePolicy) {
                policy.prioritizePolicy = prioritizePolicy;
                return this;
            }

            public Builder setDropPolicy(DropPolicy dropPolicy) {
                policy.dropPolicy = dropPolicy;
                return this;
            }

            public Builder setTimeout(long timeout) {
                policy.timeout = timeout;
                return this;
            }

            public ServicePoolingPolicy build() {
                if(!policy.isValid()) {
                    throw new IllegalStateException("invalid policy");
                }
                return policy;
            }
        }

        private boolean isValid() {
            if(dropPolicy == DropPolicy.TIMEOUT) {
                return timeout > 0L;
            }
            return true;
        }

        private PrioritizePolicy prioritizePolicy;
        private DropPolicy dropPolicy;
        private long timeout = 0L;

    }

    public static ManagedPoolServiceProxy create(ServicePoolingPolicy policy) {
        if(!policy.isValid()) {
            throw new IllegalArgumentException("invalid policy");
        }
        return null;
    }


    private ManagedPoolServiceProxy() {

    }

    public void pool(ServiceProxy proxy) {
    }

    @Override
    public void open() throws IOException, IllegalAccessException, InstantiationException {

    }

    @Override
    public boolean isOpen() {
        return false;
    }

    @Override
    public Response request(Endpoint endpoint, long timeoutMilliSec, Object... args) throws IOException {
        return null;
    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public String who() {
        return null;
    }

    @Override
    public void startQosMeasurement(long interval, long timeout, TimeUnit timeUnit, QosListener listener) {

    }


    @Override
    public void stopQosMeasurement() {

    }

    @Override
    public boolean provide(Class controller) {
        return false;
    }
}
