package net.doodream.yarmi.net;

import net.doodream.yarmi.model.RMIError;
import net.doodream.yarmi.model.RMIServiceInfo;
import net.doodream.yarmi.model.Response;
import net.doodream.yarmi.serde.Converter;
import net.doodream.yarmi.serde.Reader;
import net.doodream.yarmi.serde.Writer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SimpleNegotiator implements Negotiator {
    private static final Logger Log = LoggerFactory.getLogger(SimpleNegotiator.class);
    private static final long MAX_TIMEOUT = 10L;

    @Override
    public RMISocket handshake(RMISocket socket, RMIServiceInfo service, Converter converter, boolean isClient) throws HandshakeFailException {
        Log.info("Handshake start as {} @ {}", isClient? "CLIENT" : "SERVER", socket.getRemoteName());
        try {
            Reader reader = converter.reader(socket.getInputStream());
            Writer writer = converter.writer(socket.getOutputStream());
            if(isClient) {
                handshakeFromClient(service, reader, writer);
            } else {
                handshakeFromServer(service, reader, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return socket;
    }

    private void handshakeFromClient(final RMIServiceInfo service, Reader reader, Writer writer) throws HandshakeFailException {
        try {
            writer.write(service, MAX_TIMEOUT, TimeUnit.SECONDS);
            Log.debug("write {}", service);
            Response response = reader.read(Response.class,MAX_TIMEOUT, TimeUnit.SECONDS);
            if ((response != null) &&
                    response.isSuccessful()) {
                Log.debug("Handshake Success {} (Ver. {})", service.getName(), service.getVersion());
                return;
            }
            Log.error("Handshake Fail ({}) {}",response.getCode(), response.getBody());
        } catch (IOException e) {
            Log.error("error on read : {}", e.getMessage());
        } catch (TimeoutException e) {
            Log.error("timeout on read : {}", e.getMessage());
        }
        throw new HandshakeFailException();
    }

    private void handshakeFromServer(final RMIServiceInfo service, Reader reader, final Writer writer) throws HandshakeFailException {
        try {
            final RMIServiceInfo serviceInfo = reader.read(RMIServiceInfo.class, MAX_TIMEOUT, TimeUnit.SECONDS);
            if(serviceInfo.hashCode() == service.hashCode()) {
                writer.write(Response.success("OK"));
            } else {
                writer.write(RMIError.BAD_REQUEST.getResponse());
                throw new HandshakeFailException();
            }
        } catch (IOException e) {
            Log.error("error on handshake : {}", e.getMessage());
        } catch (TimeoutException e) {
            Log.error("timeout on handshake : {}", e.getMessage());
        }
    }

}
