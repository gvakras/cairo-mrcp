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
public interface ResourceServer extends Remote {

    public static final String NAME = "ResourceServer";

    public ResourceMessage invite(ResourceMessage request) throws ResourceUnavailableException, RemoteException;

}
