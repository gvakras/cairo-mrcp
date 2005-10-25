/**
 * TODOC
 * 
 */
package com.onomatopia.cairo.server.resource;


import java.util.List;
import java.net.InetAddress;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

/**
 * TODOC
 * @author Niels
 *
 */
public class ResourceRegistryImpl extends UnicastRemoteObject implements ResourceRegistry {

    private ResourceList _receivers = new ResourceList();
    private ResourceList _transmitters = new ResourceList();

    /**
     * TODOC
     * @throws RemoteException
     */
    public ResourceRegistryImpl() throws RemoteException {
        super();
        // TODO Auto-generated constructor stub
    }

    /**
     * TODOC
     * @param port
     * @throws RemoteException
     */
    public ResourceRegistryImpl(int port) throws RemoteException {
        super(port);
        // TODO Auto-generated constructor stub
    }

    /**
     * TODOC
     * @param port
     * @param csf
     * @param ssf
     * @throws RemoteException
     */
    public ResourceRegistryImpl(int port, RMIClientSocketFactory csf,
            RMIServerSocketFactory ssf) throws RemoteException {
        super(port, csf, ssf);
        // TODO Auto-generated constructor stub
    }

    /* (non-Javadoc)
     * @see com.onomatopia.cairo.server.manager.ResourceRegistry#hello(java.lang.String)
     */
    public String hello(String name) throws RemoteException {
        String greeting = "Hello " + name;
        System.out.println(greeting);
        //if (_resource != null) {
            //System.out.println(_resource.hello("registry"));
        //}
        return greeting;
    }
    
    /* (non-Javadoc)
     * @see com.onomatopia.cairo.server.manager.ResourceRegistry#bind(com.onomatopia.cairo.server.resource.Resource)
     */
    public synchronized void register(Resource resource, Resource.Type type) throws RemoteException {
        System.out.println("register(): registering resource of type " + type);
        switch (type) {
        case RECEIVER:
            _receivers.register(resource);
            break;

        case TRANSMITTER:
            _transmitters.register(resource);
            break;

        default:
            throw new IllegalArgumentException("Invalid type or type not specified!");
        }
    }

    public Resource getResource(Resource.Type type) throws ResourceUnavailableException {
        switch (type) {
        case RECEIVER:
            return _receivers.getResource();

        case TRANSMITTER:
            return _transmitters.getResource();

        default:
            throw new IllegalArgumentException("Invalid type or type not specified!");
        }
    }
    
    private static class ResourceList {

        private List<Resource> _resources = new ArrayList<Resource>();
        private int _index = 0;

        public synchronized void register(Resource resource) {
            _resources.add(resource);
        }

        public synchronized Resource getResource() throws ResourceUnavailableException {
            int size;
            while ((size = _resources.size()) > 0) {
                if (_index >= size) {
                    _index = 0;
                }
                Resource resource = _resources.get(_index);
                try {
                    resource.ping();
                    _index++;
                    return resource;
                } catch (RemoteException e) {
                    e.printStackTrace();
                    _resources.remove(_index);
                }
            }
            throw new ResourceUnavailableException("No resource available for the specified type!");
        }
        
    }

    public static void main(String[] args) throws Exception {
        ResourceRegistryImpl impl = new ResourceRegistryImpl();

        /*InetAddress host = InetAddress.getLocalHost();
        String url = "rmi://" + host.getHostName() + '/' + NAME;
        System.out.println("(re)binding to: " + url);
        Naming.rebind(url, impl);*/

        Registry registry = LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
        registry.rebind(NAME, impl);

        System.out.println("ResourceRegistry bound and waiting...");
        Thread.sleep(90000);
    }

    public static class TestClient {

        public static void main(String[] args) throws Exception {
            InetAddress host = InetAddress.getLocalHost();
            String url = "rmi://" + host.getHostName() + '/' + NAME;
            System.out.println("looking up: " + url);
            ResourceRegistry rr = (ResourceRegistry) Naming.lookup(url);
            System.out.println(rr.hello("Niels"));
        }

    }

}
