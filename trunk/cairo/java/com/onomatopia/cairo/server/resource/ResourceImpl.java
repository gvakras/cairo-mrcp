/**
 * TODOC
 * 
 */
package com.onomatopia.cairo.server.resource;


import com.onomatopia.cairo.server.resource.Resource.Type;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import org.mrcp4j.MrcpResourceType;

/**
 * TODOC
 * @author Niels
 *
 */
public abstract class ResourceImpl extends UnicastRemoteObject implements Resource {
    
    private Type _type;
    
    /**
     * TODOC
     * @param type whether the resource is to receive audio input or transmit audio output
     * @throws RemoteException
     */
    public ResourceImpl(Type type) throws RemoteException {
        _type = type;
    }

    /* (non-Javadoc)
     * @see com.onomatopia.cairo.server.manager.Resource#hello(java.lang.String)
     */
    public String hello(String name) {
        String greeting = "Hello " + name;
        System.out.println(greeting);
        return greeting;
    }
    
    /*protected boolean supports(MrcpResourceType resourceType) throws ResourceUnavailableException {
        Type type = translateType(resourceType);
        return type.equals(_type);
    }*/

    public static Resource.Type translateType(MrcpResourceType resourceType) throws ResourceUnavailableException {
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

}
