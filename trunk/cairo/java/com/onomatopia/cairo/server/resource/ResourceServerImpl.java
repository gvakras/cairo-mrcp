/**
 * TODOC
 * 
 */
package com.onomatopia.cairo.server.resource;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;

import org.apache.log4j.Logger;
import org.mrcp4j.MrcpResourceType;

/**
 * TODOC
 * @author Niels
 *
 */
public class ResourceServerImpl extends UnicastRemoteObject implements ResourceServer {
    
    private static Logger _logger = Logger.getLogger(ResourceServerImpl.class);

    private long _channelID = System.currentTimeMillis(); 
    private ResourceRegistryImpl _registryImpl;

    /**
     * TODOC
     * @param registryImpl 
     * @throws RemoteException
     */
    public ResourceServerImpl(ResourceRegistryImpl registryImpl) throws RemoteException {
        super();
        _registryImpl = registryImpl;
    }

    /**
     * TODOC
     * @param port
     * @param registryImpl 
     * @throws RemoteException
     */
    public ResourceServerImpl(int port, ResourceRegistryImpl registryImpl) throws RemoteException {
        super(port);
        _registryImpl = registryImpl;
    }
    
    private synchronized String getNextChannelID() { // TODO: convert from synchronized to atomic
        return Long.toHexString(_channelID++);
    }

    /* (non-Javadoc)
     * @see com.onomatopia.cairo.server.resource.ResourceServer#invite(com.onomatopia.cairo.server.resource.ResourceMessage)
     */
    public ResourceMessage invite(ResourceMessage request) throws ResourceUnavailableException, RemoteException {
        try {
            return invitePrivate(request);
        } catch (RemoteException e) {
            e.printStackTrace();
            throw e;
        } catch (ResourceUnavailableException e) {
            e.printStackTrace();
            throw e;
        }
    }

    private ResourceMessage invitePrivate(ResourceMessage request) throws ResourceUnavailableException, RemoteException {
        String channelID = getNextChannelID();

        boolean receiver = false;
        boolean transmitter = false;

        for (ResourceChannel channel : request.getChannels()) {
            MrcpResourceType resourceType = channel.getResourceType();
            channel.setChannelID(channelID + '@' + resourceType.toString());
            Resource.Type type = translateType(resourceType);
            if (type.equals(Resource.Type.RECEIVER)) {
                receiver = true;
            } else {
                transmitter = true;
            }
        }

        if (transmitter) {
            Resource resource = _registryImpl.getResource(Resource.Type.TRANSMITTER);
            request = resource.invite(request);
        }

        if (receiver) {
            Resource resource = _registryImpl.getResource(Resource.Type.RECEIVER);
            request = resource.invite(request);
        } // TODO: catch exception and release transmitter resources

        return request;
    }

    private static Resource.Type translateType(MrcpResourceType resourceType) throws ResourceUnavailableException {
        switch (resourceType) {
        case SPEECHSYNTH:
            return Resource.Type.TRANSMITTER;

        case RECORDER:
        case SPEECHRECOG:
            return Resource.Type.RECEIVER;

        default:
            throw new ResourceUnavailableException("Unsupported resource type!");
        }
    }

    /**
     * TODOC
     * @param args
     * @throws Exception 
     */
    public static void main(String[] args) throws Exception {
        ResourceRegistryImpl rr = new ResourceRegistryImpl();
        ResourceServerImpl rs = new ResourceServerImpl(rr);

        Registry registry = LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
        registry.rebind(ResourceRegistry.NAME, rr);
        registry.rebind(ResourceServer.NAME, rs);

        _logger.info("Server and registry bound and waiting...");
        //Thread.sleep(90000);

    }

}
