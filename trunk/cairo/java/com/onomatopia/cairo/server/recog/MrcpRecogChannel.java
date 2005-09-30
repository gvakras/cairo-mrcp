/**
 * TODOC
 * 
 */
package com.onomatopia.cairo.server.recog;

import com.onomatopia.cairo.server.MrcpGenericChannel;
import com.onomatopia.cairo.server.resource.ResourceUnavailableException;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.mrcp4j.MrcpEventName;
import org.mrcp4j.MrcpRequestState;
import org.mrcp4j.message.MrcpEvent;
import org.mrcp4j.message.MrcpResponse;
import org.mrcp4j.message.header.MrcpHeader;
import org.mrcp4j.message.header.MrcpHeaderFactory;
import org.mrcp4j.message.request.StartInputTimersRequest;
import org.mrcp4j.message.request.StopRequest;
import org.mrcp4j.message.request.MrcpRequestFactory.UnimplementedRequest;
import org.mrcp4j.server.MrcpSession;
import org.mrcp4j.server.provider.RecogOnlyRequestHandler;

/**
 * TODOC
 * @author Niels
 *
 */
public class MrcpRecogChannel extends MrcpGenericChannel implements RecogOnlyRequestHandler {

    private static short IDLE = 0;
    private static short RECOGNIZING = 1;
    private static short RECOGNIZED = 2;

    private RTPRecogChannel _rtpChannel;
    /*volatile*/ short _state = IDLE;
    //private String _channelID;
    private GrammarManager _grammarManager;

    /**
     * TODOC
     * @param channelID 
     * @param rtpChannel 
     * @param baseGrammarDir 
     */
    public MrcpRecogChannel(String channelID, RTPRecogChannel rtpChannel, File baseGrammarDir) {
        //_channelID = channelID;
        _rtpChannel = rtpChannel;
        _grammarManager = new GrammarManager(channelID, baseGrammarDir);
    }

    /* (non-Javadoc)
     * @see org.mrcp4j.server.provider.RecogOnlyRequestHandler#defineGrammar(org.mrcp4j.message.request.MrcpRequestFactory.UnimplementedRequest, org.mrcp4j.server.MrcpSession)
     */
    public synchronized MrcpResponse defineGrammar(UnimplementedRequest request, MrcpSession session) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.mrcp4j.server.provider.RecogOnlyRequestHandler#recognize(org.mrcp4j.message.request.MrcpRequestFactory.UnimplementedRequest, org.mrcp4j.server.MrcpSession)
     */
    public synchronized MrcpResponse recognize(UnimplementedRequest request, MrcpSession session) {
        MrcpRequestState requestState = MrcpRequestState.COMPLETE;
        short statusCode = -1;
        if (_state == RECOGNIZING) {
            statusCode = MrcpResponse.STATUS_METHOD_NOT_VALID_IN_STATE;
        } else {
            GrammarLocation grammarLocation = null;
            if (request.hasContent()) {
                String contentType = request.getContentType();
                if (contentType.equalsIgnoreCase("application/jsgf")) {
                    // save grammar to file
                    MrcpHeader contentIdHeader = request.getHeader(MrcpHeaderFactory.CONTENT_ID);
                    String grammarID = (contentIdHeader == null) ? null : contentIdHeader.getValue();
                    try {
                        grammarLocation = _grammarManager.saveGrammar(grammarID, request.getContent());
                    } catch (IOException e) {
                        e.printStackTrace();
                        statusCode = MrcpResponse.STATUS_SERVER_INTERNAL_ERROR;
                    }
                } else {
                    statusCode = MrcpResponse.STATUS_UNSUPPORTED_HEADER_VALUE;
                }
            }
            if (statusCode < 0) { // status not yet set
                try {
                    _rtpChannel.recognize(new Listener(session), false, grammarLocation); // TODO: retrieve header for boolean value
                    statusCode = MrcpResponse.STATUS_SUCCESS;
                    requestState = MrcpRequestState.IN_PROGRESS;
                    _state = RECOGNIZING;
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                    statusCode = MrcpResponse.STATUS_METHOD_NOT_VALID_IN_STATE;
                    // TODO: add completion cause header
                } catch (IOException e) {
                    e.printStackTrace();
                    statusCode = MrcpResponse.STATUS_SERVER_INTERNAL_ERROR;
                    // TODO: add completion cause header
                } catch (ResourceUnavailableException e) {
                    e.printStackTrace();
                    statusCode = MrcpResponse.STATUS_SERVER_INTERNAL_ERROR;
                    // TODO: add completion cause header
                }
            }
        }
        // TODO: cache event acceptor if request is not complete
        return session.createResponse(statusCode, requestState);
    }

    /* (non-Javadoc)
     * @see org.mrcp4j.server.provider.RecogOnlyRequestHandler#interpret(org.mrcp4j.message.request.MrcpRequestFactory.UnimplementedRequest, org.mrcp4j.server.MrcpSession)
     */
    public synchronized MrcpResponse interpret(UnimplementedRequest request, MrcpSession session) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.mrcp4j.server.provider.RecogOnlyRequestHandler#getResult(org.mrcp4j.message.request.MrcpRequestFactory.UnimplementedRequest, org.mrcp4j.server.MrcpSession)
     */
    public synchronized MrcpResponse getResult(UnimplementedRequest request, MrcpSession session) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.mrcp4j.server.provider.RecogOnlyRequestHandler#startInputTimers(org.mrcp4j.message.request.StartInputTimersRequest, org.mrcp4j.server.MrcpSession)
     */
    public synchronized MrcpResponse startInputTimers(StartInputTimersRequest request, MrcpSession session) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.mrcp4j.server.provider.RecogOnlyRequestHandler#stop(org.mrcp4j.message.request.StopRequest, org.mrcp4j.server.MrcpSession)
     */
    public synchronized MrcpResponse stop(StopRequest request, MrcpSession session) {
        // TODO Auto-generated method stub
        return null;
    }
    
    private class Listener implements RecogListener {

        private MrcpSession _session;

        /**
         * TODOC
         * @param session
         */
        public Listener(MrcpSession session) {
            _session = session;
        }

        /* (non-Javadoc)
         * @see com.onomatopia.cairo.server.recog.RecogListener#recognitionComplete()
         */
        public void recognitionComplete() {
            try {
                synchronized (MrcpRecogChannel.this) {
                    _state = RECOGNIZED;
                }
                MrcpEvent event = _session.createEvent(
                        MrcpEventName.RECOGNITION_COMPLETE,
                        MrcpRequestState.COMPLETE
                );
                _session.postEvent(event);
            } catch (IllegalStateException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (TimeoutException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        /* (non-Javadoc)
         * @see com.onomatopia.cairo.server.recog.RecogListener#speechStarted()
         */
        public void speechStarted() {
            try {
                //TODO: check state before posting event
                MrcpEvent event = _session.createEvent(
                        MrcpEventName.START_OF_INPUT,
                        MrcpRequestState.IN_PROGRESS
                );
                _session.postEvent(event);
            } catch (IllegalStateException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (TimeoutException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
    }

}
