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
package org.speechforge.cairo.server.recog.sphinx;

import org.speechforge.cairo.server.recog.GrammarLocation;
import org.speechforge.cairo.server.recog.RecogListener;
import org.speechforge.cairo.server.recog.RecogListenerDecorator;
import org.speechforge.cairo.server.recog.RecognitionResult;
import org.speechforge.cairo.server.recog.SpeechEventListener;
import org.speechforge.cairo.server.rtp.PBDSReplicator;
import org.speechforge.cairo.util.jmf.ProcessorStarter;
import org.speechforge.cairo.util.pool.AbstractPoolableObject;

import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;

import javax.media.CannotRealizeException;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.NoDataSourceException;
import javax.media.NoProcessorException;
import javax.media.Processor;
import javax.media.ProcessorModel;
import javax.media.StartEvent;
import javax.media.format.AudioFormat;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;
import javax.speech.recognition.GrammarException;
import javax.speech.recognition.RuleGrammar;
import javax.speech.recognition.RuleParse;

import edu.cmu.sphinx.jsapi.JSGFGrammar;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;

import org.apache.log4j.Logger;

/**
 * TODOC
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 *
 */
public class SphinxRecEngine extends AbstractPoolableObject implements SpeechEventListener {

    static Logger _logger = Logger.getLogger(SphinxRecEngine.class);
    private static Toolkit _toolkit = _logger.isTraceEnabled()? Toolkit.getDefaultToolkit() : null;

    private Recognizer _recognizer;
    private JSGFGrammar _jsgfGrammar;
    private RawAudioProcessor _rawAudioProcessor;

    private RawAudioTransferHandler _rawAudioTransferHandler;
    RecogListener _recogListener;

    protected SphinxRecEngine(ConfigurationManager cm)
      throws IOException, PropertyException, InstantiationException {

        _recognizer = (Recognizer) cm.lookup("recognizer");
        _recognizer.allocate();

        _jsgfGrammar = (JSGFGrammar) cm.lookup("jsgfGrammar");

        SpeechDataMonitor speechDataMonitor = (SpeechDataMonitor) cm.lookup("speechDataMonitor");
        if (speechDataMonitor != null) {
            speechDataMonitor.setSpeechEventListener(this);
        }

        Object primaryInput = cm.lookup("primaryInput");
        if (primaryInput instanceof RawAudioProcessor) {
            _rawAudioProcessor = (RawAudioProcessor) primaryInput;
        } else {
            String className = (primaryInput == null) ? null : primaryInput.getClass().getName();
            throw new InstantiationException("Unsupported primary input type: " + className);
        }
    }

    /* (non-Javadoc)
     * @see org.speechforge.cairo.util.pool.PoolableObject#activate()
     */
    @Override
    public synchronized void activate() {
        _logger.debug("SphinxRecEngine activating...");
    }

    /* (non-Javadoc)
     * @see org.speechforge.cairo.util.pool.PoolableObject#passivate()
     */
    @Override
    public synchronized void passivate() {
        _logger.debug("SphinxRecEngine passivating...");
        stopProcessing();
        _recogListener = null;
    }

    /**
     * TODOC
     */
    public synchronized void stopProcessing() {
        _logger.debug("SphinxRecEngine stopping processing...");
        if (_rawAudioTransferHandler != null) {
            _rawAudioTransferHandler.stopProcessing();
            _rawAudioTransferHandler = null;
        }
        // TODO: should wait to set this until after run thread completes (i.e. recognizer is cleared)
    }

    /**
     * TODOC
     * @param grammarLocation
     * @throws IOException
     * @throws GrammarException
     */
    public synchronized void loadJSGF(GrammarLocation grammarLocation) throws IOException, GrammarException {
        _jsgfGrammar.setBaseURL(grammarLocation.getBaseURL());
        try {
            _jsgfGrammar.loadJSGF(grammarLocation.getGrammarName());
            _logger.debug("loadJSGF(): completed successfully.");
        } catch (com.sun.speech.engine.recognition.TokenMgrError e) {
            _logger.debug("loadJSGF(): encountered exception: " + e.getClass().getName(), e); // com.sun.speech.engine.recognition.TokenMgrError!!!
            String message = e.getMessage();
            /*if (message.indexOf("speech") < 0) {
                throw e;
            }*/
            // else assume caused by GrammarException
            // TODO: edu.cmu.sphinx.jsapi.JSGFGrammar.loadJSGF() should be updated not to swallow GrammarException
            throw new GrammarException(message);
        }
    }

    /**
     * TODOC
     * @param text
     * @param ruleName
     * @return
     * @throws GrammarException
     */
    public synchronized RuleParse parse(String text, String ruleName) throws GrammarException {
        if (_rawAudioTransferHandler != null) {
            throw new IllegalStateException("Recognition already in progress!");
        }
        
        RuleGrammar ruleGrammar = _jsgfGrammar.getRuleGrammar();
        return ruleGrammar.parse(text, ruleName);
    }

    /**
     * TODOC
     * @param dataSource
     * @param listener 
     * @throws UnsupportedEncodingException 
     */
    public synchronized void startRecognition(PushBufferDataSource dataSource, RecogListener listener)
      throws UnsupportedEncodingException {

        if (_rawAudioTransferHandler != null) {
            throw new IllegalStateException("Recognition already in progress!");
        }

        PushBufferStream[] streams = dataSource.getStreams();
        if (streams.length != 1) {
            throw new IllegalArgumentException(
                "Rec engine can handle only single stream datasources, # of streams: " + streams);
        }
        _logger.debug("starting recognition");
        try {
            _rawAudioTransferHandler = new RawAudioTransferHandler(_rawAudioProcessor);
            _rawAudioTransferHandler.startProcessing(streams[0]);
        } catch (UnsupportedEncodingException e) {
            _rawAudioTransferHandler = null;
            throw e;
        }

        _recogListener = listener;
    }

    // TODO: rename method
    public void startRecogThread() {
        new RecogThread().start();
    }

    private RecognitionResult waitForResult() {
        Result result = _recognizer.recognize();
        stopProcessing();
        if (result != null) {
            Result result2clear = _recognizer.recognize();
            if (result2clear != null) {
                _logger.debug("waitForResult(): result2clear not null!");
            }
        } else {
            _logger.debug("waitForResult(): got null result from recognizer!");
            return null;
        }
        return new RecognitionResult(result);
    }

    /* (non-Javadoc)
     * @see org.speechforge.cairo.server.recog.SpeechEventListener#speechStarted()
     */
    public void speechStarted() {
        if (_toolkit != null) {
            _toolkit.beep();
        }

        RecogListener recogListener = null;
        synchronized (this) {
            recogListener = _recogListener; 
        }

        if (recogListener == null) {
            _logger.debug("speechStarted(): _recogListener is null!");
        } else {
            recogListener.speechStarted();
        }
    }

    /* (non-Javadoc)
     * @see org.speechforge.cairo.server.recog.SpeechEventListener#speechEnded()
     */
    public void speechEnded() {
        if (_toolkit != null) {
            _toolkit.beep();
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // inner classes
    ///////////////////////////////////////////////////////////////////////////

    private class RecogThread extends Thread {
        
        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
            _logger.debug("RecogThread waiting for result...");

            RecognitionResult result = SphinxRecEngine.this.waitForResult();

            if (_logger.isDebugEnabled()) {
                StringBuilder sb = new StringBuilder();
                sb.append("\n**************************************************************");
                sb.append("\nRecogThread got result: ").append(result);
                sb.append("\n**************************************************************");
                _logger.debug(sb);
            }
            
            RecogListener recogListener = null;
            synchronized (SphinxRecEngine.this) {
                recogListener = _recogListener;
            }

            if (recogListener == null) {
                _logger.debug("RecogThread.run(): _recogListener is null!");
            } else {
                recogListener.recognitionComplete(result);
            }
        }
    }

    /**
     * Provides main method for testing SphinxRecEngine in standalone mode using the microphone for input.
     */
    public static class Test extends RecogListenerDecorator {

        private static MediaLocator MICROPHONE = new MediaLocator("dsound://");
        private static AudioFormat[] PREFERRED_MEDIA_FORMATS = {SourceAudioFormat.PREFERRED_MEDIA_FORMAT};
        private static final ContentDescriptor CONTENT_DESCRIPTOR_RAW = new ContentDescriptor(ContentDescriptor.RAW);

        private SphinxRecEngine _engine;
        private RecognitionResult _result;
        private PBDSReplicator _replicator;

        public Test(SphinxRecEngine engine)
          throws NoProcessorException, NoDataSourceException, CannotRealizeException, IOException {
            super(null);
            _engine = engine;
            _replicator = createMicrophoneReplicator();
        }

        /* (non-Javadoc)
         * @see org.speechforge.cairo.server.recog.RecogListener#recognitionComplete(org.speechforge.cairo.server.recog.RecognitionResult)
         */
        @Override
        public synchronized void recognitionComplete(RecognitionResult result) {
            _result = result;
            this.notify();
        }

        public RecognitionResult doRecognize() throws IOException, NoProcessorException, CannotRealizeException,
                InterruptedException {

            _result = null;
            _engine.activate();

            Processor processor = createReplicatedProcessor();
            processor.addControllerListener(new ProcessorStarter());

            PushBufferDataSource pbds = (PushBufferDataSource) processor.getDataOutput();
            _engine.startRecognition(pbds, this);
            processor.start();
            _logger.debug("Performing recognition...");
            _engine.startRecogThread();

            // wait for result
            RecognitionResult result = null;
            synchronized (this) {
                while (_result == null) {
                    this.wait(1000);
                }
                result = _result;
                _result = null;
            }

            _engine.passivate();

            return result;
        }

        private Processor createReplicatedProcessor() throws IOException,
                IllegalStateException, NoProcessorException,
                CannotRealizeException {
            
            ProcessorModel pm = new ProcessorModel(
                    _replicator.replicate(),
                    PREFERRED_MEDIA_FORMATS,
                    CONTENT_DESCRIPTOR_RAW
            );
            
            _logger.debug("Creating realized processor...");
            Processor processor = Manager.createRealizedProcessor(pm);
            _logger.debug("Processor realized.");
            
            return processor;
        }

        private static Processor createMicrophoneProcessor()
          throws NoDataSourceException, IOException, NoProcessorException, CannotRealizeException {

            DataSource dataSource = Manager.createDataSource(MICROPHONE);
            ProcessorModel pm = new ProcessorModel(dataSource,
                    PREFERRED_MEDIA_FORMATS, CONTENT_DESCRIPTOR_RAW);
            Processor processor = Manager.createRealizedProcessor(pm);
            return processor;
        }

        private static PBDSReplicator createMicrophoneReplicator()
          throws NoProcessorException, NoDataSourceException, CannotRealizeException, IOException {
            Processor processor = createMicrophoneProcessor();
            processor.addControllerListener(new ProcessorStarter());
            PushBufferDataSource pbds = (PushBufferDataSource) processor.getDataOutput();
            PBDSReplicator replicator = new PBDSReplicator(pbds);
            processor.start();
            return replicator;
        }
        
        public static void main(String[] args) throws Exception {
            URL url;
            if (args.length > 0) {
                url = new File(args[0]).toURL();
            } else {
                url = SphinxRecEngine.class.getResource("/config/sphinx-config.xml");
            }
            
            if (url == null) {
                throw new RuntimeException("Sphinx config file not found!");
            }

            System.out.println("Loading...");
            ConfigurationManager cm = new ConfigurationManager(url);
            SphinxRecEngine engine = new SphinxRecEngine(cm);

            for (int i=0; i < 12; i++) {
                System.out.println(engine._jsgfGrammar.getRandomSentence());
            }
            Test test = new Test(engine);
            

            RecognitionResult result;
            while (true) {
                result = test.doRecognize();
            }

//            RuleParse ruleParse = engine.parse("", "main");


            //System.exit(0);
        }

    }

}
