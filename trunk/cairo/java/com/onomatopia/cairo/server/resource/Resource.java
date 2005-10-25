/**
 * TODOC
 * 
 */
package com.onomatopia.cairo.server.resource;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * TODOC
 * @author Niels
 *
 */
public interface Resource extends Remote {

    //public static final String NAME = "Resource";

    public void ping() throws RemoteException;

    public ResourceMessage invite(ResourceMessage request) throws ResourceUnavailableException, RemoteException;

    /**
     * Whether the resource receives audio input or transmits audio output.
     * 
     * @author Niels
     *
     */
    public static enum Type {
        RECEIVER,
        TRANSMITTER
    }

}
