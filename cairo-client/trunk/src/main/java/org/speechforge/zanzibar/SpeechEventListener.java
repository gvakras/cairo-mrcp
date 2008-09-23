package org.speechforge.zanzibar;

import org.mrcp4j.message.MrcpEvent;
import org.speechforge.zanzibar.recog.RecognitionResult;

// TODO: Auto-generated Javadoc

/**
 * Callback interface for getting recognition and synthesis/tts results.
 * 
 * @author Spencer Lord {@literal <}<a href="mailto:salord@users.sourceforge.net">salord@users.sourceforge.net</a>{@literal >}
 */
public interface SpeechEventListener {

    
    /**
     * Recognition event received.
     * 
     * @param event the mrcp event
     * @param r the recognition result
     */
    public void recognitionEventReceived(MrcpEvent event, RecognitionResult r);
    
    /**
     * Tts completed event received.
     * 
     * @param event the mrcp event
     */
    public void ttsCompletedEventReceived(MrcpEvent event);

    

    public enum EventType {recognitionMatch, noInputTimeout, noMatchTimeout}
    
    /**
     * Character event received.  Most typically used for DTMF (in which case valid characters include 0-9, * and #)
     * 
     * @param c the charcater received
     */
    public void characterEventReceived(String c, EventType status);
    
}
