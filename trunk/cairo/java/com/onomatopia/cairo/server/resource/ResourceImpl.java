/*
 * Cairo - Open source framework for control of speech media resources.
 *
 * Copyright (C) 2005 Onomatopia, Inc. - http://www.onomatopia.com
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * Contact: ngodfredsen@users.sourceforge.net
 *
 */
package com.onomatopia.cairo.server.resource;


import com.onomatopia.cairo.server.resource.Resource.Type;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import org.mrcp4j.MrcpResourceType;

/**
 * TODOC
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
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
