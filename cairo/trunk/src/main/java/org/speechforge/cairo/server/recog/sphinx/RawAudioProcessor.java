package com.onomatopia.cairo.server.recog.sphinx;

import com.onomatopia.cairo.util.BlockingFifoQueue;
import com.onomatopia.cairo.util.ByteHexConverter;

import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.media.format.AudioFormat;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DataStartSignal;
import edu.cmu.sphinx.frontend.util.Utterance;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.PropertyType;
import edu.cmu.sphinx.util.props.Registry;

import org.apache.log4j.Logger;


/**
 * <p>
 * A Microphone captures audio data from the system's underlying
 * audio input systems. Converts these audio data into Data
 * objects. When the method <code>startProcessing()</code> is called,
 * a new thread will be created and used to capture
 * audio, and will stop when <code>stopProcessing()</code>
 * is called. Calling <code>getData()</code> returns the captured audio
 * data as Data objects.
 * </p>
 * <p>
 * This Microphone will attempt to obtain an audio device with the format
 * specified in the configuration. If such a device with that format
 * cannot be obtained, it will try to obtain a device with an audio format
 * that has a higher sample rate than the configured sample rate,
 * while the other parameters of the format (i.e., sample size, endianness,
 * sign, and channel) remain the same. If, again, no such device can be
 * obtained, it flags an error, and a call <code>startProcessing</code> 
 * returns false.
 * </p>
 */
public class RawAudioProcessor extends BaseDataProcessor
  implements /*SessionListener, ReceiveStreamListener, ControllerListener, BufferTransferHandler,*/ Runnable {

    private static Logger _logger = Logger.getLogger(RawAudioTransferHandler.class);

    /**
     * The Sphinx property that specifies the number of milliseconds of
     * audio data to read each time from the underlying Java Sound audio 
     * device.
     */
    public final static String PROP_MSEC_PER_READ = "msecPerRead";

    /**
     * The default value of PROP_MSEC_PER_READ.
     */
    public final static int PROP_MSEC_PER_READ_DEFAULT = 10;

    private BlockingFifoQueue<Data> _dataList;
    private BlockingFifoQueue<byte[]> _rawAudioList;
    private AudioFormat _mediaFormat;
    private SourceAudioFormat _audioFormat;
    private AudioDataTransformer _transformer = null;
    private Utterance _currentUtterance;
    private volatile boolean _processing = false;
    private volatile boolean _utteranceEndReached = false;
    private volatile byte[] _frame;
    private volatile int _framePointer = 0;
    private FileWriter _fileWriter = null;

    // Configuration data

    //private java.util.logging.Logger _logger;
    //private boolean closeBetweenUtterances;
    /*private boolean keepDataReference;*/
    private int _msecPerRead;

    // Runnable variables

    private long _totalSamplesRead = 0;
    private long _startTime;


    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#register(java.lang.String,
     *      edu.cmu.sphinx.util.props.Registry)
     */
    public void register(String name, Registry registry)
        throws PropertyException {
        super.register(name, registry);
        registry.register(PROP_MSEC_PER_READ, PropertyType.INT);
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
     */
    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);
        //_logger = ps.getLogger();

        _msecPerRead = ps.getInt(PROP_MSEC_PER_READ, 
                                PROP_MSEC_PER_READ_DEFAULT);

    }

    /**
     * Constructs a Microphone with the given InputStream.
     */
    public void initialize() {
        super.initialize();
        _dataList = new BlockingFifoQueue<Data>();
        _rawAudioList = new BlockingFifoQueue<byte[]>();
    }

    /**
     * Returns true if this Microphone is processing.
     *
     * @return true if this Microphone is processing, false otherwise
     */
    public synchronized boolean isProcessing() {
        return _processing;
    }

    /**
     * Starts processing the audio data being added to the addRawData method.
     * @param format format of the audio being passed to this processor
     * @throws UnsupportedEncodingException if the specified format cannot be supported
     */
    public synchronized void startProcessing(AudioFormat format) throws UnsupportedEncodingException {
        if (_processing) {
            throw new IllegalStateException("RawAudioProcessor.startProcessing() cannot be called while already in processing state!");
        }

        try {
            //_fileWriter = new FileWriter("C:\\work\\cvs\\onomatopia\\cairo\\prompts\\test\\rtp.txt", false);
        } catch (Exception e) {
            _logger.warn(e, e);
        }


        //_mediaFormat = format;
        _audioFormat = SourceAudioFormat.newInstance(_msecPerRead, format);
        if (_logger.isDebugEnabled()) {
            _logger.debug("Frame size: " + _audioFormat.getFrameSizeInBytes() + " bytes");
        }
        _utteranceEndReached = false;
        //_transformer = new AudioDataTransformer(_audioFormat, stereoToMono, selectedChannel);
        _transformer = new AudioDataTransformer(_audioFormat, "average", 0);
        _frame = new byte[_audioFormat.getFrameSizeInBytes()];

        Thread processingThread = new Thread(this);
        processingThread.start();

        _processing = true;
    }


    /**
     * Stops processing audio. This method does not return until processing
     * has been stopped and all data has been read from the audio line.
     */
    public synchronized void stopProcessing() {
        _processing = false;

        if (_framePointer > 0) {
            // write final frame
            byte[] finalFrame = new byte[_framePointer];
            for (int i = 0; i < _framePointer; i++) {
                finalFrame[i] = _frame[i];
            }
            _rawAudioList.add(finalFrame);
        }
        // add end signal
        _rawAudioList.add(new byte[0]);

        if (_fileWriter != null) {
            try {
                _fileWriter.close();
                _fileWriter = null;
            } catch (IOException e){
                _logger.warn(e, e);
            }
        }

    }

    /**
     * Implements the run() method of the Thread class.
     * Records audio, and cache them in the audio buffer.
     */
    public void run() {            
        _totalSamplesRead = 0;
        _startTime = System.currentTimeMillis();
        _logger.debug("started processing");
        
        /*if (keepDataReference) {
            currentUtterance = new Utterance
                (this.getName(), audioFormat);
        }*/
        
        try {
            Data data = new DataStartSignal();
            _logger.debug("adding DataStartSignal...");
            do {
                _dataList.add(data);
                data = readData(null);//currentUtterance);
            } while (data != null);
        } catch (InterruptedException e) {
            _logger.warn(e, e);
        } 

        /*long duration = (long)
            (((double)totalSamplesRead/
              (double)audioFormat.getSampleRate())*1000.0);*/
        
        _dataList.add(new DataEndSignal(_audioFormat.calculateDurationMsecs(_totalSamplesRead)));
        _logger.debug("DataEndSignal added");

        /*synchronized (lock) {
            lock.notify();
        }*/
    }

    /**
     * Reads one frame of audio data, and adds it to the given Utterance.
     *
     * @return an Data object containing the audio data
     */
    private Data readData(Utterance utterance) throws InterruptedException {

        _logger.trace("readData(): retrieving data from raw audio list...");

        byte[] data = _rawAudioList.remove();

        if (_logger.isTraceEnabled()) {
            _logger.trace("readData(): data from raw audio list, bytes=" + data.length);
        }

        long firstSampleNumber = _totalSamplesRead / _audioFormat.getChannels();
        long collectTime = _startTime + (firstSampleNumber * _audioFormat.getMsecPerRead());

        //  notify the waiters upon start
        /*if (!started) {
            synchronized (this) {
                started = true;
                notifyAll();
            }
        }*/

        if (data.length < 1) {
            return null;
        }

        _totalSamplesRead += (data.length / _audioFormat.getSampleSizeInBytes());
        
        if (data.length != _audioFormat.getFrameSizeInBytes()) {
            if (data.length % _audioFormat.getSampleSizeInBytes() != 0) {
                throw new Error("Incomplete sample read.");
            }
        }
        
        /*if (keepDataReference) {
            utterance.add(data);
        }*/

        return _transformer.toDoubleData(data, collectTime, firstSampleNumber);
    }


    /**
     * Reads and returns the next Data object from this
     * Microphone, return null if there is no more audio data.
     * All audio data captured in-between <code>startRecording()</code>
     * and <code>stopRecording()</code> is cached in an Utterance
     * object. Calling this method basically returns the next
     * chunk of audio data cached in this Utterance.
     *
     * @return the next Data or <code>null</code> if none is
     *         available
     *
     * @throws DataProcessingException if there is a data processing error
     */
    public Data getData() throws DataProcessingException {

        //getTimer().start();
        /*try {
            throw new Exception("debugging stack for RawAudioProcessor.getData()");
        } catch (Exception e){
            _logger.warn(e, e);
        }

java.lang.Exception: debugging stack for RawAudioProcessor.getData()
	at com.onomatopia.cairo.server.recog.sphinx.RawAudioProcessor.getData(RawAudioProcessor.java:339)
	at edu.cmu.sphinx.frontend.endpoint.SpeechClassifier.getData(SpeechClassifier.java:241)
	at edu.cmu.sphinx.frontend.endpoint.SpeechMarker.readData(SpeechMarker.java:204)
	at edu.cmu.sphinx.frontend.endpoint.SpeechMarker.getData(SpeechMarker.java:171)
	at edu.cmu.sphinx.frontend.endpoint.NonSpeechDataFilter.readData(NonSpeechDataFilter.java:344)
	at edu.cmu.sphinx.frontend.endpoint.NonSpeechDataFilter.getData(NonSpeechDataFilter.java:176)
	at edu.cmu.sphinx.frontend.filter.Preemphasizer.getData(Preemphasizer.java:103)
	at edu.cmu.sphinx.frontend.window.RaisedCosineWindower.getData(RaisedCosineWindower.java:201)
	at edu.cmu.sphinx.frontend.transform.DiscreteFourierTransform.getData(DiscreteFourierTransform.java:304)
	at edu.cmu.sphinx.frontend.frequencywarp.MelFrequencyFilterBank.getData(MelFrequencyFilterBank.java:361)
	at edu.cmu.sphinx.frontend.transform.DiscreteCosineTransform.getData(DiscreteCosineTransform.java:123)
	at edu.cmu.sphinx.frontend.feature.LiveCMN.getData(LiveCMN.java:163)
	at edu.cmu.sphinx.frontend.feature.DeltasFeatureExtractor.getData(DeltasFeatureExtractor.java:138)
	at edu.cmu.sphinx.frontend.FrontEnd.getData(FrontEnd.java:241)
	at edu.cmu.sphinx.decoder.scorer.ThreadedAcousticScorer.calculateScores(ThreadedAcousticScorer.java:234)
	at edu.cmu.sphinx.decoder.search.SimpleBreadthFirstSearchManager.scoreTokens(SimpleBreadthFirstSearchManager.java:337)
	at edu.cmu.sphinx.decoder.search.SimpleBreadthFirstSearchManager.recognize(SimpleBreadthFirstSearchManager.java:258)
	at edu.cmu.sphinx.decoder.search.SimpleBreadthFirstSearchManager.recognize(SimpleBreadthFirstSearchManager.java:226)
	at edu.cmu.sphinx.decoder.Decoder.decode(Decoder.java:94)
	at edu.cmu.sphinx.recognizer.Recognizer.recognize(Recognizer.java:116)
	at edu.cmu.sphinx.recognizer.Recognizer.recognize(Recognizer.java:135)
	at com.onomatopia.cairo.server.recognition.RecServlet.run(RecServlet.java:110)
	at java.lang.Thread.run(Thread.java:595)

        */

        Data output = null;

        if (!_utteranceEndReached) {
            try {
                output = _dataList.remove();
            } catch (InterruptedException e){
                _logger.warn(e, e);
                throw (DataProcessingException) new DataProcessingException("Data processing thread interrupted!").initCause(e);
            }
            if (output instanceof DataEndSignal) {
                _utteranceEndReached = true;
            }
        }

        //getTimer().stop();

        // signalCheck(output);

        if (_logger.isTraceEnabled()) {
            _logger.trace("RawAudioProcessor.getData() returning data.");
        }

        return output;
    }


    /* (non-Javadoc)
     * @see com.onomatopia.cairo.server.recog.sphinx.RawAudioListener#addRawData(byte[])
     */
    public synchronized void addRawData(byte[] data) {
        addRawData(data, 0, data.length);
    }

    public synchronized void addRawData(byte[] data, int offset, int length) {
        if (!_processing) {
            throw new IllegalStateException("Attempt to add raw data when RawAudioProcessor not in processing state!");
        }

        if (_logger.isTraceEnabled()) {
            _logger.trace("addRawData(): offset=" + offset + ", length=" + length);
        }

        if (_fileWriter != null) {
            try {
                ByteHexConverter.writeHexDigits(_fileWriter, data, offset, length);
            } catch (IOException e){
                _logger.warn(e, e);
            }
        }

        int dataPointer = offset;
        length = length + offset;
        if (length > data.length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        while (true) {
            while (_framePointer < _frame.length && dataPointer < length) {
                _frame[_framePointer++] = data[dataPointer++];
            }
            if (_framePointer == _frame.length) {
                // the frame was filled
                _rawAudioList.add(_frame);
                _frame = new byte[_audioFormat.getFrameSizeInBytes()];
                _framePointer = 0;
            } else {
                // the data buffer was exhausted
                break;
            }
        }

        if (_logger.isTraceEnabled()) {
            _logger.trace("remainder = " + _framePointer);
        }

    }

}
