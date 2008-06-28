package org.speechforge.cairo.util.sip;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;

import org.apache.commons.configuration.ConfigurationException;
import org.speechforge.cairo.exception.ResourceUnavailableException;
import org.speechforge.cairo.server.config.CairoConfig;
import org.speechforge.cairo.server.config.ReceiverConfig;
import org.speechforge.cairo.server.config.TransmitterConfig;
import org.speechforge.cairo.server.resource.ReceiverResource;
import org.speechforge.cairo.server.resource.Resource;
import org.speechforge.cairo.server.resource.ResourceRegistry;
import org.speechforge.cairo.server.resource.TransmitterResource;
import org.speechforge.cairo.server.resource.Resource.Type;
import org.speechforge.cairo.util.CairoUtil;

public class MockResourceRegistryImpl implements ResourceRegistry {

    public Resource getResource(Type type) throws ResourceUnavailableException {
        Resource r = null;

        URL configURL = null;
        try {
            configURL = CairoUtil.argToURL("src/main/resources/config/cairo-config.xml");
        } catch (MalformedURLException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        CairoConfig config = null;
        TransmitterConfig tConfig = null;
        ReceiverConfig rConfig = null;
        try {
            config = new CairoConfig(configURL);
        } catch (ConfigurationException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        try {
            tConfig = config.getTransmitterConfig("transmitter1");
            rConfig = config.getReceiverConfig("receiver1");
        } catch (ConfigurationException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        try {
            if (type == Resource.Type.RECEIVER) {
                r = new ReceiverResource(rConfig);
            } else {
                r = new TransmitterResource(tConfig);
            }
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InstantiationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return r;
    }

    public String hello(String name) throws RemoteException {
        // TODO Auto-generated method stub
        return null;
    }

    public void register(Resource resource, Type type) throws RemoteException {
        // TODO Auto-generated method stub

    }

}
