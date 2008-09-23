package org.speechforge.zanzibar;

/**
 * SpeechClientProvider API.  This interface is implemented by SpeechClient and is used by the underlying infrastructure.  The basic idea is that
 * the SpeechClient interface is used by applications to do speech processing.  The implementations of the speech cleint must implement the
 * SpeechClientProvider interface so that the underlying platform can notify it of events (such as a dtmf signal being recived).  These methods are
 * not visibale to the application using the speech client.
 * 
 * @author Spencer Lord {@literal <}<a href="mailto:salord@users.sourceforge.net">salord@users.sourceforge.net</a>{@literal >}
 */
public interface SpeechClientProvider {
    
    public void characterEventReceived(char code);
    
    public void bye();

}
