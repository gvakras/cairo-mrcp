/*
 * Cairo - Open source framework for control of speech media resources.
 *
 * Copyright (C) 2005 Onomatopia, Inc. - http://www.onomatopia.com
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * Contact: ngodfredsen@users.sourceforge.net
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
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
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
