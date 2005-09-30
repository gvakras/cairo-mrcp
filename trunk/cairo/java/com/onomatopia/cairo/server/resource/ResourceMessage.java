/**
 * TODOC
 * 
 */
package com.onomatopia.cairo.server.resource;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * TODOC
 * @author Niels
 *
 */
public class ResourceMessage implements Serializable {
    
    private String _callId = null;
    private List<ResourceChannel> _channels = new ArrayList<ResourceChannel>();
    private ResourceMediaStream _mediaStream;
    /**
     * TODOC
     * @return Returns the callId.
     */
    public String getCallId() {
        return _callId;
    }
    /**
     * TODOC
     * @param callId The callId to set.
     */
    public void setCallId(String callId) {
        _callId = callId;
    }
    /**
     * TODOC
     * @return Returns the channels.
     */
    public List<ResourceChannel> getChannels() {
        return _channels;
    }
    /**
     * TODOC
     * @param channels The channels to set.
     */
    public void setChannels(List<ResourceChannel> channels) {
        _channels = channels;
    }
    /**
     * TODOC
     * @return Returns the mediaStream.
     */
    public ResourceMediaStream getMediaStream() {
        return _mediaStream;
    }
    /**
     * TODOC
     * @param mediaStream The mediaStream to set.
     */
    public void setMediaStream(ResourceMediaStream mediaStream) {
        _mediaStream = mediaStream;
    }

    /**
     * TODOC
     * /
    public ResourceMessage() {
        super();
        // TODO Auto-generated constructor stub
    }*/

}
