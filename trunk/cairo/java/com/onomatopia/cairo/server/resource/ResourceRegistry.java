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
public interface ResourceRegistry extends Remote {

    public static final String NAME = "ResourceRegistry";

    public String hello(String name) throws RemoteException;

    public void register(Resource resource, Resource.Type type) throws RemoteException;

}
