/**
 * TODOC
 * 
 */
package com.onomatopia.cairo.server;

import org.mrcp4j.message.MrcpResponse;
import org.mrcp4j.message.request.MrcpRequestFactory.UnimplementedRequest;
import org.mrcp4j.server.MrcpSession;
import org.mrcp4j.server.provider.GenericRequestHandler;

/**
 * TODOC
 * @author Niels
 *
 */
public abstract class MrcpGenericChannel implements GenericRequestHandler {

    /* (non-Javadoc)
     * @see org.mrcp4j.server.provider.GenericRequestHandler#setParams(org.mrcp4j.message.request.MrcpRequestFactory.UnimplementedRequest, org.mrcp4j.server.MrcpSession)
     */
    public MrcpResponse setParams(UnimplementedRequest request, MrcpSession session) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.mrcp4j.server.provider.GenericRequestHandler#getParams(org.mrcp4j.message.request.MrcpRequestFactory.UnimplementedRequest, org.mrcp4j.server.MrcpSession)
     */
    public MrcpResponse getParams(UnimplementedRequest request, MrcpSession session) {
        // TODO Auto-generated method stub
        return null;
    }

}
