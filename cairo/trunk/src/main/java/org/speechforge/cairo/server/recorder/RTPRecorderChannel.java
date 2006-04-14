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
package org.speechforge.cairo.server.recorder;

import org.speechforge.cairo.server.rtp.RTPStreamReplicator;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import javax.media.DataSink;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.Processor;
import javax.media.datasink.DataSinkEvent;
import javax.media.datasink.DataSinkListener;
import javax.media.datasink.EndOfStreamEvent;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import javax.media.protocol.FileTypeDescriptor;

import org.apache.commons.lang.Validate;


/**
 * TODOC
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 *
 */
public class RTPRecorderChannel implements DataSinkListener {

    //private static AudioFormat[] PREFERRED_MEDIA_FORMATS = {SourceAudioFormat.PREFERRED_MEDIA_FORMAT};

    private static final ContentDescriptor CONTENT_DESCRIPTOR_WAVE =
        new FileTypeDescriptor(FileTypeDescriptor.WAVE);

    private File _dir;
    private RTPStreamReplicator _replicator;

    private Processor _processor;
    private File _destination;

    /**
     * TODOC
     * @param dir directory to save recorded files to
     * @param replicator 
     * @throws IllegalArgumentException if the File specified is not a directory
     */
    public RTPRecorderChannel(File dir, RTPStreamReplicator replicator) throws IllegalArgumentException {
        Validate.notNull(dir, "Null directory!");
        Validate.isTrue(dir.isDirectory(), "File object specified must be a directory: ", dir);
        // TODO: make subdirectory based on channel ID 
        Validate.notNull(replicator, "Null replicator!");

        _dir = dir;
        _replicator = replicator;
    }

    /**
     * Starts recording the current RTP stream to an audio file
     * 
     * @param startInputTimers whether to start input timers or wait for a future command to start input timers
     * @return the location of the recorded file
     * @throws IOException
     * @throws IllegalStateException
     */
    public synchronized File startRecording(boolean startInputTimers) throws IOException, IllegalStateException {
        if (_processor != null) {
            throw new IllegalStateException("Recording already in progress!");
        }
        
        _processor = _replicator.createRealizedProcessor(CONTENT_DESCRIPTOR_WAVE, 2000); // TODO: specify audio format

        DataSource dataSource = _processor.getDataOutput();
        if (dataSource == null) {
            throw new IOException("Processor.getDataOutput() returned null!");
        }

        _destination = new File(_dir, new StringBuilder().append(System.currentTimeMillis()).append(".wav").toString());

        try {
            DataSink dataSink = Manager.createDataSink(dataSource, new MediaLocator(_destination.toURL()));
            dataSink.addDataSinkListener(this);
            System.out.println("contentType=" + dataSink.getContentType());
            dataSink.open();
            System.out.println("opened datasink...");
            _processor.start();
            dataSink.start();
            System.out.println("started processor...");
            System.out.println("started datasink...");
        } catch (javax.media.NoDataSinkException e){
            throw (IOException) new IOException(e.getMessage()).initCause(e);
        } catch (MalformedURLException e){
            throw (IOException) new IOException(e.getMessage()).initCause(e);
        }

        return _destination;

    }
    
    /**
     * Stops recording the current RTP stream and closes the audio file
     * 
     * @return the location of the recorded file
     * @throws IllegalStateException if recording is not yet in progress
     */
    public synchronized File stopRecording() throws IllegalStateException {
        if (_processor == null) {
            throw new IllegalStateException("Recording not in progress!");
        }
        System.err.println("Closing processor...");
        _processor.close();
        System.err.println("Processor closed.");
        _processor = null;

        // TODO: wait for EndOfStreamEvent

        return _destination;
    }

    /* (non-Javadoc)
     * @see javax.media.datasink.DataSinkListener#dataSinkUpdate(javax.media.datasink.DataSinkEvent)
     */
    public void dataSinkUpdate(DataSinkEvent event) {
        System.out.println("DataSinkEvent received: " + event);

        if (event instanceof EndOfStreamEvent) {
            event.getSourceDataSink().close();
            System.out.println("closed datasink...");
        }
    }

    @Deprecated
    private class TestThread extends Thread {
        
        @Override
        public void run() {
            try {
                Thread.sleep(1000);
                System.out.println("TestThread: start recording...");
                startRecording(false);
                Thread.sleep(3000);
                System.out.println("TestThread: stop recording...");
                stopRecording();
                Thread.sleep(1000);
                System.out.println("TestThread: start recording...");
                startRecording(false);
                Thread.sleep(2000);
                System.out.println("TestThread: stop recording...");
                stopRecording();
                System.out.println("TestThread: complete.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
    }

}
