/**
 * TODOC
 * 
 */
package com.onomatopia.cairo.server.resource;

import java.io.Serializable;

import org.mrcp4j.MrcpResourceType;

/**
 * TODOC
 * @author Niels
 *
 */
public class ResourceChannel implements Serializable {
    
    private MrcpResourceType _resourceType = null;
    private String _channelID = null;
    private int _port = -1;

    /**
     * TODOC
     */
    public ResourceChannel() {
        super();
        // TODO Auto-generated constructor stub
    }

    /**
     * TODOC
     * @param resourceType The resourceType to set.
     */
    public void setResourceType(MrcpResourceType resourceType) {
        _resourceType = resourceType;
    }

    /**
     * TODOC
     * @return Returns the resourceType.
     */
    public MrcpResourceType getResourceType() {
        return _resourceType;
    }

    /**
     * TODOC
     * @param channelID The channelID to set.
     */
    public void setChannelID(String channelID) {
        _channelID = channelID;
    }

    /**
     * TODOC
     * @return Returns the channelID.
     */
    public String getChannelID() {
        return _channelID;
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
