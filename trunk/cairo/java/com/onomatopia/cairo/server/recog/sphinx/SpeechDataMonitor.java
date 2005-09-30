/**
 * TODOC
 * 
 */
package com.onomatopia.cairo.server.recog.sphinx;

import com.onomatopia.cairo.server.recog.SpeechEventListener;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.endpoint.SpeechEndSignal;
import edu.cmu.sphinx.frontend.endpoint.SpeechStartSignal;

/**
 * TODOC
 * @author Niels
 *
 */
public class SpeechDataMonitor extends BaseDataProcessor {
    
    private SpeechEventListener _speechEventListener = null;

    /**
     * TODOC
     */
    public SpeechDataMonitor() {
        super();
        // TODO Auto-generated constructor stub
    }
    
    public void setSpeechEventListener(SpeechEventListener speechEventListener) {
        _speechEventListener = speechEventListener;
    }

    /* (non-Javadoc)
     * @see edu.cmu.sphinx.frontend.BaseDataProcessor#getData()
     */
    @Override
    public Data getData() throws DataProcessingException {
        Data data = getPredecessor().getData();
        if (data instanceof SpeechStartSignal) {
            broadcastSpeechStartSignal();
        } else if (data instanceof SpeechEndSignal) {
            broadcastSpeechEndSignal();
        }
        return data;
    }
    
    private void broadcastSpeechStartSignal() {
        System.out.println("\n*************** SpeechStartSignal encountered!\n");
        if (_speechEventListener != null) {
            _speechEventListener.speechStarted();
        }
    }

    private void broadcastSpeechEndSignal() {
        System.out.println("\n*************** SpeechEndSignal encountered!\n");
        if (_speechEventListener != null) {
            _speechEventListener.speechEnded();
        }
    }

}
