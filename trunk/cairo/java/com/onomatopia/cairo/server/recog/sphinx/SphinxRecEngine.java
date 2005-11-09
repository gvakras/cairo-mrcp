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

import com.onomatopia.cairo.server.recog.GrammarLocation;
import com.onomatopia.cairo.server.recog.RecogListener;
import com.onomatopia.cairo.server.recog.RecognitionResult;
import com.onomatopia.cairo.server.recog.SpeechEventListener;

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

/**
 * TODOC
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 *
 */
public class SphinxRecEngine implements SpeechEventListener {

    private Recognizer _recognizer;
    private JSGFGrammar _jsgfGrammar;
    private RawAudioProcessor _rawAudioProcessor;

    private RawAudioTransferHandler _rawAudioTransferHandler;
    private RecogListener _recogListener;

    protected SphinxRecEngine(ConfigurationManager cm)
      throws IOException, PropertyException, InstantiationException {

        _recognizer = (Recognizer) cm.lookup("recognizer");
        _recognizer.allocate();

        _jsgfGrammar = (JSGFGrammar) cm.lookup("jsgfGrammar");

        try {
            SpeechDataMonitor speechDataMonitor = (SpeechDataMonitor) cm.lookup("speechDataMonitor");
            speechDataMonitor.setSpeechEventListener(this);
        } catch (InstantiationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (PropertyException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        Object primaryInput = cm.lookup("primaryInput");
        if (primaryInput instanceof RawAudioProcessor) {
            _rawAudioProcessor = (RawAudioProcessor) primaryInput;
        } else {
            throw new InstantiationException("Unsupported primary input type: " + primaryInput.getClass().getName());
        }
    }

    public synchronized void activate() /*throws Exception*/ {
        System.out.println("SphinxRecEngine activating...");
    }

    public synchronized void passivate() {
        System.out.println("SphinxRecEngine passivating...");
        stopProcessing();
    }

    public synchronized void stopProcessing() {
        System.out.println("SphinxRecEngine stopping processing...");
        if (_rawAudioTransferHandler != null) {
            _rawAudioTransferHandler.stopProcessing();
            _rawAudioTransferHandler = null;
        }
        // TODO: should wait to set this until after run thread completes (i.e. recognizer is cleared)
    }

    public synchronized void loadJSGF(GrammarLocation grammarLocation) throws IOException, GrammarException {
        _jsgfGrammar.setBaseURL(grammarLocation.getBaseURL());
        try {
            _jsgfGrammar.loadJSGF(grammarLocation.getGrammarName());
            System.out.println("loadJSGF(): completed successfully.");
        } catch (com.sun.speech.engine.recognition.TokenMgrError e) {
            System.out.println("loadJSGF(): encountered exception: " + e.getClass().getName()); // com.sun.speech.engine.recognition.TokenMgrError!!!
            String message = e.getMessage();
            /*if (message.indexOf("speech") < 0) {
                throw e;
            }*/
            // else assume caused by GrammarException
            // TODO: edu.cmu.sphinx.jsapi.JSGFGrammar.loadJSGF() should be updated not to swallow GrammarException
            e.printStackTrace();
            throw new GrammarException(message);
        }
    }

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
     * @throws UnsupportedEncodingException 
     */
    public synchronized void startRecognition(PushBufferDataSource dataSource)
      throws UnsupportedEncodingException {

        if (_rawAudioTransferHandler != null) {
            throw new IllegalStateException("Recognition already in progress!");
        }

        PushBufferStream[] streams = dataSource.getStreams();
        if (streams.length != 1) {
            throw new IllegalArgumentException(
                "Rec engine can handle only single stream datasources, # of streams: " + streams);
        }
        try {
            _rawAudioTransferHandler = new RawAudioTransferHandler(_rawAudioProcessor);
            _rawAudioTransferHandler.startProcessing(streams[0]);
        } catch (UnsupportedEncodingException e) {
            _rawAudioTransferHandler = null;
            throw e;
        }
    }

    /**
     * TODOC
     * @param listener 
     * @return
     */
    public RecognitionResult waitForResult(RecogListener listener) {
        _recogListener = listener;
        Result result = _recognizer.recognize();
        stopProcessing();
        if (result != null) {
            Result result2clear = _recognizer.recognize();
            if (result2clear != null) {
                System.out.println("waitForResult(): result2clear not null!");
            }
        } else {
            System.out.println("waitForResult(): got null result from recognizer!");
            return null;
        }
        return new RecognitionResult(result);
    }

    /* (non-Javadoc)
     * @see com.onomatopia.cairo.server.recog.SpeechEventListener#speechStarted()
     */
    public void speechStarted() {
        if (_recogListener != null) {
            _recogListener.speechStarted();
        }
    }

    /* (non-Javadoc)
     * @see com.onomatopia.cairo.server.recog.SpeechEventListener#speechEnded()
     */
    public void speechEnded() {
        // TODO Auto-generated method stub
        
    }

    private static MediaLocator MICROPHONE = new MediaLocator("dsound://");
    private static AudioFormat[] PREFERRED_MEDIA_FORMATS = {SourceAudioFormat.PREFERRED_MEDIA_FORMAT};
    private static final ContentDescriptor CONTENT_DESCRIPTOR_RAW = new ContentDescriptor(ContentDescriptor.RAW);

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

        RecognitionResult result;
        while (true) {
            result = doRecognize(engine);
        }

//        RuleParse ruleParse = engine.parse("", "main");


        //System.exit(0);
    }

    private static RecognitionResult doRecognize(SphinxRecEngine engine)
      throws NoDataSourceException, IOException, NoProcessorException, CannotRealizeException {

        engine.activate();

        DataSource dataSource = Manager.createDataSource(MICROPHONE);
        ProcessorModel pm = new ProcessorModel(dataSource, PREFERRED_MEDIA_FORMATS, CONTENT_DESCRIPTOR_RAW);
        Processor processor = Manager.createRealizedProcessor(pm);
        processor.addControllerListener(new Listener());

        PushBufferDataSource pbds = (PushBufferDataSource) processor.getDataOutput();
        engine.startRecognition(pbds);
        processor.start();
        System.out.println("Performing recognition...");
        RecognitionResult result = engine.waitForResult(null);
        System.out.println("**************************************************\n" +
                           "result: " + result + "\n" +
                           "**************************************************");

        engine.passivate();

        return result;
    }
    
    private static class Listener implements ControllerListener {

        /* (non-Javadoc)
         * @see javax.media.ControllerListener#controllerUpdate(javax.media.ControllerEvent)
         */
        public void controllerUpdate(ControllerEvent event) {
            System.out.println("RTPRecogChannel: ControllerEvent received: " + event);
            try {
                if (event instanceof StartEvent) {
                    Processor processor = (Processor) event.getSourceController();
                    DataSource dataSource = processor.getDataOutput();
                    System.out.println("Starting data source...");
                    dataSource.connect();
                    dataSource.start();
                //} else if (event instanceof StopEvent) { //EndOfMediaEvent) {
                    //event.getSourceController().close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }

}
