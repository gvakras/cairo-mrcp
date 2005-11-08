/**
 * TODOC
 * 
 */
package com.onomatopia.cairo.server.resource;

import java.rmi.Remote;
import java.rmi.RemoteException;

import org.mrcp4j.MrcpResourceType;

/**
 * TODOC
 * @author Niels
 *
 */
public interface Resource extends Remote {

    public void ping() throws RemoteException;

    public ResourceMessage invite(ResourceMessage request) throws ResourceUnavailableException, RemoteException;

    /**
     * Whether the resource receives audio input or transmits audio output.
     * 
     * @author Niels
     *
     */
    public static enum Type {

        /**
         * Resource that receives RTP data.
         */
        RECEIVER,

        /**
         * Resource that transmits RTP data.
         */
        TRANSMITTER;

        /**
         * Converts an MRCP resource type to a Cairo resource type.
         * @param resourceType the MRCP resource type.
         * @return the resource type (RECEIVER or TRANSMITTER) corresponding to the provide MRCP resource type. 
         * @throws ResourceUnavailableException if the MRCP resource type is not supported by Cairo.
         */
        public static Resource.Type fromMrcpType(MrcpResourceType resourceType) throws ResourceUnavailableException {
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

}
