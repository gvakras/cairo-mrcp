/**
 * TODOC
 * 
 */
package com.onomatopia.cairo.server.recog.sphinx;

import com.onomatopia.cairo.server.recog.GrammarLocation;
import com.onomatopia.cairo.server.recog.RecogListener;
import com.onomatopia.cairo.server.recog.RecognitionResult;
import com.onomatopia.cairo.server.recog.SpeechEventListener;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;

import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;

import edu.cmu.sphinx.jsapi.JSGFGrammar;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;

/**
 * TODOC
 * @author Niels
 *
 */
public class SphinxRecEngine implements SpeechEventListener {

    private Recognizer _recognizer;
    private JSGFGrammar _jsgfGrammarManager;
    private RawAudioProcessor _rawAudioProcessor;

    private RawAudioTransferHandler _rawAudioTransferHandler;
    private RecogListener _recogListener;

    public SphinxRecEngine(ConfigurationManager cm)
      throws IOException, PropertyException, InstantiationException {

        _recognizer = (Recognizer) cm.lookup("recognizer");
        _recognizer.allocate();

        _jsgfGrammarManager = (JSGFGrammar) cm.lookup("jsgfGrammar");
        
        SpeechDataMonitor speechDataMonitor = (SpeechDataMonitor) cm.lookup("speechDataMonitor");
        speechDataMonitor.setSpeechEventListener(this);

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

    public synchronized void loadJSGF(GrammarLocation grammarLocation) throws IOException {
        _jsgfGrammarManager.setBaseURL(grammarLocation.getBaseURL());
        _jsgfGrammarManager.loadJSGF(grammarLocation.getGrammarName());
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


}
