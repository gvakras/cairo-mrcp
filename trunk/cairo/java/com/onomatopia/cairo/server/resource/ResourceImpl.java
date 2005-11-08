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
    public void ping() {
        System.out.println("Resource received ping() request.");
    }
    
    /*protected boolean supports(MrcpResourceType resourceType) throws ResourceUnavailableException {
        Type type = translateType(resourceType);
        return type.equals(_type);
    }*/


}
