package org.speechforge.zanzibar;

import org.speechforge.zanzibar.recog.RecognitionResult;


/**
 * This object maintains the state of a speech request.
 * 
 * @author Spencer Lord {@literal <}<a href="mailto:salord@users.sourceforge.net">salord@users.sourceforge.net</a>{@literal >}
 */
public class SpeechRequest {
    
    public enum RequestType {play, recognize, playAndRecognize}
    
    private long requestId;
    private boolean completed;
    private SpeechEventListener listener;
    private boolean blockingCall = false;
    private RecognitionResult result;
    private RequestType requestType;
    private SpeechRequest linkedRequest;
    //TODO: Add status of the call that initiated the request. 
    

    public SpeechRequest(long requestId, RequestType type, boolean completed, SpeechEventListener listener) {
        super();
        this.requestId = requestId;
        this.requestType = type;
        this.completed = completed;
        this.listener = listener;
    }
    
    
    /**
     * @return the completed
     */
    public boolean isCompleted() {
        return completed;
    }
    
    /**
     * @param completed the completed to set
     */
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    /**
     * @return the listener
     */
    public SpeechEventListener getListener() {
        return listener;
    }
    
    /**
     * @param listener the listener to set
     */
    public void setListener(SpeechEventListener listener) {
        this.listener = listener;
    }
    
    /**
     * @return the requestId
     */
    public long getRequestId() {
        return requestId;
    }
    
    /**
     * @param requestId the requestId to set
     */
    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }
    
    /**
     * @return the blockingCall
     */
    public boolean isBlockingCall() {
        return blockingCall;
    }
    
    /**
     * @param blockingCall the blockingCall to set
     */
    public void setBlockingCall(boolean blockingCall) {
        this.blockingCall = blockingCall;
    }

    /**
     * @return the result
     */
    public RecognitionResult getResult() {
        return result;
    }
    
    /**
     * @param result the result to set
     */
    public void setResult(RecognitionResult result) {
        this.result = result;
    }
    
    /**
     * @return the requestType
     */
    public RequestType getRequestType() {
        return requestType;
    }
    
    /**
     * @param requestType the requestType to set
     */
    public void setRequestType(RequestType requestType) {
        this.requestType = requestType;
    }
    
    /**
     * @return the linkedRequest
     */
    public SpeechRequest getLinkedRequest() {
        return linkedRequest;
    }
    
    /**
     * @param linkedRequest the linkedRequest to set
     */
    public void setLinkedRequest(SpeechRequest linkedRequest) {
        this.linkedRequest = linkedRequest;
    } 
}

