/*
 * Cairo - Open source framework for control of speech media resources.
 *
 * Copyright (C) 2005-2006 SpeechForge - http://www.speechforge.org
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
package org.speechforge.cairo.server.recog;

import org.speechforge.cairo.server.recog.sphinx.SphinxRecEngine;
import org.speechforge.cairo.server.resource.ResourceUnavailableException;
import org.speechforge.cairo.server.rtp.RTPStreamReplicator;
import org.speechforge.cairo.util.jmf.ProcessorStarter;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import javax.media.Processor;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.PushBufferDataSource;
import javax.speech.recognition.GrammarException;

import org.apache.commons.lang.Validate;
import org.apache.commons.pool.ObjectPool;
import org.apache.log4j.Logger;

/**
 * TODOC
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 *
 */
public class RTPRecogChannel {

    public static final short WAITING_FOR_SPEECH = 0;
    public static final short SPEECH_IN_PROGRESS = 1;
    public static final short COMPLETE = 2;

    static Logger _logger = Logger.getLogger(RTPRecogChannel.class);

    /**
     * Content descriptor for raw audio content.
     */
    private static final ContentDescriptor CONTENT_DESCRIPTOR_RAW =
        new ContentDescriptor(ContentDescriptor.RAW);

    private /*static*/ Timer _timer = new Timer();

    private ObjectPool _recEnginePool;
    private RTPStreamReplicator _replicator;

    RecogListener _recogListener;
    SphinxRecEngine _recEngine = null;
    TimerTask _noInputTimeoutTask;

    private Processor _processor;

    volatile short _state = COMPLETE;

    /**
     * TODOC
     * @param recEnginePool 
     * @param replicator 
     */
    public RTPRecogChannel(ObjectPool recEnginePool, RTPStreamReplicator replicator) {
        Validate.notNull(recEnginePool, "Null recEnginePool!");
        Validate.notNull(replicator, "Null replicator!");

        _recEnginePool = recEnginePool;
        _replicator = replicator;
    }

    /**
     * TODOC
     * @param listener
     * @param grammarLocation
     * @param noInputTimeout
     * @throws IllegalStateException
     * @throws IOException
     * @throws ResourceUnavailableException
     * @throws GrammarException
     */
    public synchronized void recognize(RecogListener listener, GrammarLocation grammarLocation, long noInputTimeout)
      throws IllegalStateException, IOException, ResourceUnavailableException, GrammarException {

        if (_processor != null) {
            throw new IllegalStateException("Recognition already in progress!");
        }

        _processor = _replicator.createRealizedProcessor(CONTENT_DESCRIPTOR_RAW, 10000); // TODO: specify audio format

        PushBufferDataSource dataSource = (PushBufferDataSource) _processor.getDataOutput();
        if (dataSource == null) {
            throw new IOException("Processor.getDataOutput() returned null!");
        }

        try {
            _logger.debug("Borrowing recognition engine from object pool...");
            _recEngine = (SphinxRecEngine) _recEnginePool.borrowObject();
        } catch (Exception e) {
            _logger.debug(e, e);
            closeProcessor();
            throw new ResourceUnavailableException("All rec engines are in use!", e);
            // TODO: wait for availability...?
        }

        _recogListener = new Listener(listener);
        
        try {
            _logger.debug("Loading grammar...");
            _recEngine.loadJSGF(grammarLocation);

            _logger.debug("Starting recognition...");
            _state = WAITING_FOR_SPEECH;
            _recEngine.startRecognition(dataSource, _recogListener);

            _processor.addControllerListener(new ProcessorStarter());
            _processor.start();

            _recEngine.startRecogThread();

            if (noInputTimeout > 0) {
                startInputTimers(noInputTimeout);
            }

        } catch (GrammarException e) {
            closeProcessor();
            throw e;
        } catch (IOException e) {
            closeProcessor();
            throw e;
        }
    }

    /**
     * Starts the input timers which trigger no-input-timeout if speech has not started after the specified time.
     * @param noInputTimeout the amount of time to wait, in milliseconds, before triggering a no-input-timeout. 
     * @return <@code true> if input timers were started or <@code false> if speech has already started.
     * @throws IllegalStateException if recognition is not in progress or if the input timers have already been started.
     */
    public synchronized boolean startInputTimers(long noInputTimeout) throws IllegalStateException {
        if (noInputTimeout <= 0) {
            throw new IllegalArgumentException("Illegal value for no-input-timeout: " + noInputTimeout);
        }
        if (_processor == null) {
            throw new IllegalStateException("Recognition not in progress!");
        }
        if (_noInputTimeoutTask != null) {
            throw new IllegalStateException("InputTimer already started!");
        }

        boolean startInputTimers = (_state == WAITING_FOR_SPEECH); 
        if (startInputTimers) {
            _noInputTimeoutTask = new NoInputTimeoutTask();
            _timer.schedule(_noInputTimeoutTask, noInputTimeout);
        }

        return startInputTimers;
    }

    synchronized void closeProcessor() {
        if (_processor != null) {
          _logger.debug("Closing processor...");
            _processor.close();
            _processor = null;
        }
        if (_recEngine != null) {
            try {
                _recEnginePool.returnObject(_recEngine);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                _logger.debug(e, e);
            }
            _recEngine = null;
        }
    }

    private class NoInputTimeoutTask extends TimerTask {

        /* (non-Javadoc)
         * @see java.util.TimerTask#run()
         */
        @Override
        public void run() {
            synchronized (RTPRecogChannel.this) {
                _noInputTimeoutTask = null;
                if (_state == WAITING_FOR_SPEECH) {
                    _state = COMPLETE;
                    closeProcessor();
                    if (_recogListener != null) {
                        _recogListener.noInputTimeout();
                    }
                }
            }
        }
        
    }

    private class Listener extends RecogListenerDecorator {

        /**
         * TODOC
         * @param recogListener
         */
        public Listener(RecogListener recogListener) {
            super(recogListener);
        }

        /* (non-Javadoc)
         * @see org.speechforge.cairo.server.recog.RecogListener#speechStarted()
         */
        @Override
        public void speechStarted() {
            synchronized (RTPRecogChannel.this) {
                if (_state == WAITING_FOR_SPEECH) {
                    _state = SPEECH_IN_PROGRESS;
                }
                if (_noInputTimeoutTask != null) {
                    _noInputTimeoutTask.cancel();
                }
            }
            super.speechStarted();
        }

        /* (non-Javadoc)
         * @see org.speechforge.cairo.server.recog.RecogListener#recognitionComplete()
         */
        @Override
        public void recognitionComplete(RecognitionResult result) {
            boolean doit = false;
            synchronized (RTPRecogChannel.this) {
                if (_state == SPEECH_IN_PROGRESS) {
                    _state = COMPLETE;
                    doit = true;
                }
            }
            if (doit) {
                closeProcessor();
                super.recognitionComplete(result);
            }
        }

//        public void noInputTimeout() {
//            boolean doit = false;
//            synchronized (RTPRecogChannel.this) {
//                doit = _speechStarted;
//                _speechStarted = false;
//            }
//            try {
//                _recEnginePool.returnObject(_recEngine);
//            } catch (Exception e) {
//                // TODO Auto-generated catch block
//                _logger.debug(e, e);
//            }
//            super.noInputTimeout();
//        }

    }
}
