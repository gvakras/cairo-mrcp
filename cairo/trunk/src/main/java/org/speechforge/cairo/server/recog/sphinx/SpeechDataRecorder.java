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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.Player;
import javax.media.protocol.DataSource;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.speechforge.cairo.server.recog.SpeechEventListener;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DataStartSignal;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.frontend.FloatData;
import edu.cmu.sphinx.frontend.endpoint.SpeechEndSignal;
import edu.cmu.sphinx.frontend.endpoint.SpeechStartSignal;

import org.apache.log4j.Logger;

/**

 * Monitors a stream of speech data being processed and broadcasts start-of-speech and end-of-speech events.
 *
 * @author Spencer Lord {@literal <}<a href="salord@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 */
public class SpeechDataRecorder extends BaseDataProcessor {

    private static Logger _logger = Logger.getLogger(SpeechDataRecorder.class);

    private SpeechEventListener _speechEventListener = null;

    private ByteArrayOutputStream baos;
    private DataOutputStream dos;

    private boolean isInSpeech;
    private boolean captureUtts = true;
    
    
    /**
     * TODOC
     */
    public SpeechDataRecorder() {
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
            isInSpeech = true;
            _logger.debug("*************** SpeechStartSignal encountered!");
        } else if (data instanceof SpeechEndSignal) {
            stopRecordingData(data);
            _logger.debug("*************** SpeechEndSignal encountered!");
        } else if (data instanceof DataStartSignal) {
            baos = new ByteArrayOutputStream();
            dos = new DataOutputStream(baos);
            _logger.debug("<<<<<<<<<<<<<<< DataStartSignal encountered!");
        } else if (data instanceof DataEndSignal) {
            _logger.debug(">>>>>>>>>>>>>>> DataEndSignal encountered!");
        }
        
        if ((isInSpeech) && (data instanceof DoubleData || data instanceof FloatData)) {
            DoubleData dd = data instanceof DoubleData ? (DoubleData) data : FloatData2DoubleData((FloatData) data);
            double[] values = dd.getValues();

//            if (isBigEndian) {
//                doubleData = DataUtil.bytesToValues(samplesBuffer, 0, totalRead, bytesPerValue, signedData);
//            } else {
//                doubleData = DataUtil.littleEndianBytesToValues(samplesBuffer, 0, totalRead, bytesPerValue, signedData);
//            }

            for (double value : values) {
                try {
                    dos.writeShort(new Short((short) value));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }        
        return data;
    }
    
    /** Converts DoubleData object to FloatDatas. */
    public static DoubleData FloatData2DoubleData(FloatData data) {
        int numSamples = data.getValues().length;

        double[] doubleData = new double[numSamples];
        float[] values = data.getValues();
        for (int i = 0; i < values.length; i++) {
            doubleData[i] = values[i];
        }
//        System.arraycopy(data.getValues(), 0, doubleData, 0, numSamples); 

        return new DoubleData(doubleData, data.getSampleRate(), data.getCollectTime(), data.getFirstSampleNumber());
    }
    
 
    private void stopRecordingData(Data data) {
        
        String dumpFilePath = "c:/temp/";

        int bitsPerSample = 16;
        int sampleRate = 8000;
        boolean isBigEndian = true;
        boolean isSigned = true;

        AudioFormat wavFormat = new AudioFormat(sampleRate, bitsPerSample, 1, isSigned, isBigEndian);
        AudioFileFormat.Type outputType = getTargetType("wav");

        System.out.println("created audio Format Object "+wavFormat.toString());
        
        
        String wavName = dumpFilePath + getNextFreeIndex(dumpFilePath) + ".wav";
        System.out.println("filename:" + wavName);

        byte[] abAudioData = baos.toByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream(abAudioData);
        AudioInputStream ais = new AudioInputStream(bais, wavFormat, abAudioData.length / wavFormat.getFrameSize());

        File outWavFile = new File(wavName);

        if (AudioSystem.isFileTypeSupported(outputType, ais)) {
            try {
                AudioSystem.write(ais, outputType, outWavFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
           System.out.println("output type not supported..."); 
        }
        System.out.println("Can read?: "+ outWavFile.canRead() );
        /*Player player;
        try {
            MediaLocator source = new MediaLocator(outWavFile.toURL());
            DataSource dataSource = Manager.createDataSource(source);
            player = Manager.createRealizedPlayer(dataSource);
            player.start();
            player.close();
            player.deallocate();
            player = null;
        } catch (Exception e) {
            _logger.warn("Could not create player for new stream!", e);
        }
        _logger.debug("Starting player..."); */

        
        isInSpeech = false;
    }
    
    private static AudioFileFormat.Type getTargetType(String extension) {
        AudioFileFormat.Type[] typesSupported = AudioSystem.getAudioFileTypes();

        for (AudioFileFormat.Type aTypesSupported : typesSupported) {
            if (aTypesSupported.getExtension().equals(extension)) {
                return aTypesSupported;
            }
        }

        return null;
    }
    
    private static int getNextFreeIndex(String outPattern) {
        int fileIndex = 0;
        while (new File(outPattern + fileIndex + ".wav").isFile())
            fileIndex++;

        return fileIndex;
    }

}
