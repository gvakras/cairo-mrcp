/**
 * TODOC
 * 
 */
package com.onomatopia.cairo.server.tts;

/**
 * TODOC
 * @author Niels
 *
 */
@Deprecated public class SpeechState {

    public static final short IDLE = 0;
    public static final short SPEAKING = 1;
    public static final short PAUSED = 2;

    private short _state = IDLE;

    /**
     * TODOC
     */
    public SpeechState() {
        super();
    }

    /**
     * TODOC
     * @param state The state to set.
     */
    public synchronized void setState(short state) {
        _state = state;
    }

    /**
     * TODOC
     * @return Returns the state.
     */
    public synchronized short getState() {
        return _state;
    }

    /**
     * TODOC
     * @return
     */
    public synchronized boolean isIdle() {
        return _state == IDLE;
    }

}
