/**
 * TODOC
 * 
 */
package com.onomatopia.cairo.server.resource;

import java.io.Serializable;

/**
 * TODOC
 * @author Niels
 *
 */
public class ResourceMediaStream implements Serializable {
    
    private int _port = -1;

    /**
     * TODOC
     */
    public ResourceMediaStream() {
        super();
        // TODO Auto-generated constructor stub
    }

    /**
     * TODOC
     * @param port The port to set.
     */
    public void setPort(int port) {
        _port = port;
    }

    /**
     * TODOC
     * @return Returns the port.
     */
    public int getPort() {
        return _port;
    }

}
