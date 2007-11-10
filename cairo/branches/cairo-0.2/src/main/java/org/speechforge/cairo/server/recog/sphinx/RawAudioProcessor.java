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

import org.speechforge.cairo.util.BlockingFifoQueue;
import org.speechforge.cairo.util.ByteHexConverter;

import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.media.format.AudioFormat;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DataStartSignal;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.PropertyType;
import edu.cmu.sphinx.util.props.Registry;

import org.apache.log4j.Logger;

/**
 * Processes raw audio data input and feeds it to the frontend of the Sphinx recognition engine.
 * 
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 *
 */
public class RawAudioProcessor extends BaseDataProcessor implements Runnable {

    private static Logger _logger = Logger.getLogger(RawAudioProcessor.class);

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
    private SourceAudioFormat _audioFormat;
    private AudioDataTransformer _transformer = null;
    private volatile boolean _processing = false;
    private volatile boolean _utteranceEndReached = false;
    private volatile byte[] _frame;
    private volatile int _framePointer = 0;
    private FileWriter _fileWriter = null;

    // Configuration data

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

    /* (non-Javadoc)
     * @see edu.cmu.sphinx.frontend.DataProcessor#initialize()
     */
    public void initialize() {
        super.initialize();
        _dataList = new BlockingFifoQueue<Data>();
        _rawAudioList = new BlockingFifoQueue<byte[]>();
    }

    /**
     * Whether this processor is currently processing audio.
     *
     * @return true if this processor is currently in the processing state.
     */
    public synchronized boolean isProcessing() {
        return _processing;
    }

    /**
     * Starts processing any audio data being added via this processors
     * {@link org.speechforge.cairo.server.recog.sphinx.RawAudioProcessor#addRawData(byte[], int, int)} method.
     * 
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
        _logger.debug("stopProcessing() called: adding final frame data and end signal...");
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
     * Processes all audio data to be tra.
     */
    public void run() {            
        _totalSamplesRead = 0;
        _startTime = System.currentTimeMillis();
        _logger.debug("started processing");
        try {
            Data data = new DataStartSignal();
            _logger.debug("adding DataStartSignal...");
            do {
                _dataList.add(data);
                data = transformNextRawAudio();
            } while (data != null);
        } catch (InterruptedException e) {
            _logger.warn(e, e);
        } 

        _dataList.add(new DataEndSignal(_audioFormat.calculateDurationMsecs(_totalSamplesRead)));
        _logger.debug("DataEndSignal added");

        /*synchronized (lock) {
            lock.notify();
        }*/
    }

    private Data transformNextRawAudio() throws InterruptedException {

        _logger.trace("transformNextRawAudio(): retrieving data from raw audio list...");

        byte[] data = _rawAudioList.remove();

        if (_logger.isTraceEnabled()) {
            _logger.trace("transformNextRawAudio(): data from raw audio list, bytes=" + data.length);
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

        return _transformer.toDoubleData(data, collectTime, firstSampleNumber);
    }

    /* (non-Javadoc)
     * @see edu.cmu.sphinx.frontend.DataProcessor#getData()
     */
    public Data getData() throws DataProcessingException {

        //getTimer().start();

        Data output = null;

        if (!_utteranceEndReached) {
            try {
                _logger.trace("getData(): getting data from data list...");
                output = _dataList.remove();
                _logger.trace("getData(): got data from data list.");
            } catch (InterruptedException e){
                _logger.warn(e, e);
                throw (DataProcessingException) new DataProcessingException("Data processing thread interrupted!").initCause(e);
            }
            if (output instanceof DataEndSignal) {
                _utteranceEndReached = true;
            }
        }

        //getTimer().stop();

        return output;
    }

    /**
     * After processing is started on this processor this will add raw audio data to be processed.
     * 
     * @param data buffer of raw audio data to be processed
     */
    public synchronized void addRawData(byte[] data) {
        addRawData(data, 0, data.length);
    }

    /**
     * After processing is started on this processor this will add raw audio data to be processed.
     * 
     * @param data buffer of raw audio data to be processed
     * @param offset starting point in buffer to process data from
     * @param length number of bytes to be processed from buffer
     */
    public synchronized void addRawData(byte[] data, int offset, int length) {
        try {
            addRawDataPrivate(data, offset, length);
        } catch (RuntimeException e) {
            _logger.debug("addRawData(): throwing exception", e);
            throw e;
        }
    }

    private synchronized void addRawDataPrivate(byte[] data, int offset, int length) {
        if (!_processing) {
            throw new IllegalStateException("Attempt to add raw data when RawAudioProcessor not in processing state!");
        }

        if (_logger.isTraceEnabled()) {
            _logger.trace("addRawData(): offset=" + offset + ", length=" + length);
        }

        if (length < 1) {
            _logger.debug("addRawData(): no data to add (length < 1).");
            return;
        }

        if (data == null) {
            throw new IllegalArgumentException("Attempt to call addRawData() passing null data argument!");
        }

        if (offset + length > data.length) {
            throw new IllegalArgumentException("Attempt to call addRawData() with offset plus length greater than data length!");
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

    public static RawAudioProcessor getInstanceForTesting(){
        RawAudioProcessor instance = new RawAudioProcessor();
        instance._msecPerRead = PROP_MSEC_PER_READ_DEFAULT;
        instance.initialize();
        return instance;
    }

}
