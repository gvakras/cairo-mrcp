/**
 * 
 */
package com.onomatopia.cairo.server.rtp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.media.Buffer;
import javax.media.Duration;
import javax.media.Format;
import javax.media.Time;
import javax.media.protocol.BufferTransferHandler;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;

/**
 * @author Niels
 *
 */
public class PBDSReplicator implements BufferTransferHandler {

    static final Object[] EMPTY_CONTROLS_ARRAY = new Object[0];

    List<PBDS> _destinations = new ArrayList<PBDS>();
    PushBufferDataSource _pbds;
    Format _format;

    /**
     * TODOC
     * @param pbds a data source to be replicated.  This will attempt to connect to and start the data source.
     * @throws IOException if there are I/O problems when connecting to or starting the data source
     */
    public PBDSReplicator(PushBufferDataSource pbds) throws IOException {
        _pbds = pbds;
        PushBufferStream[] pbStreams = _pbds.getStreams();
        if (pbStreams.length != 1) {
            if (pbStreams.length > 1) {
                System.out.println("Only first stream handled, total streams received: " + pbStreams.length);
                for (int i = 0; i < pbStreams.length; i++) {
                    ContentDescriptor cd = pbStreams[i].getContentDescriptor();
                    System.out.println("stream " + i + " content type: " + cd.getContentType());
                    System.out.println("stream " + i + " format: " + pbStreams[i].getFormat());
                }
            } else {
                throw new IOException("PushBufferDataSource.getStreams() returned zero streams!");
            }
        }

        pbStreams[0].setTransferHandler(this);
        _format = pbStreams[0].getFormat();
        _pbds.connect();
        _pbds.start();
    }
    
    public PushBufferDataSource replicate() {
        PBDS pbds = new PBDS();
        synchronized (_destinations) {
            _destinations.add(pbds);
        }
        return pbds;
    }

    /* (non-Javadoc)
     * @see javax.media.protocol.BufferTransferHandler#transferData(javax.media.protocol.PushBufferStream)
     */
    public void transferData(PushBufferStream pbs) {
        Buffer buffer = new Buffer();
        IOException ioe = null;

        try {
            pbs.read(buffer);
        } catch (IOException e) {
            ioe = e;
            e.printStackTrace();
        }
        
        synchronized (_destinations) {
            for (PBDS pbds : _destinations) {
                pbds.newData(buffer, ioe);
            }
        }
    }

    /**
     * @author Niels
     *
     */
    private class PBDS extends PushBufferDataSource implements PushBufferStream {

        private BufferTransferHandler _bufferTransferHandler;
        private Buffer _buffer;
        private IOException _ioe;
        private volatile boolean _started = false;

        /**
         * @param buffer
         * @param ioe
         */
        public synchronized void newData(Buffer buffer, IOException ioe) {
            if (_started) {
                _buffer = buffer;
                _ioe = ioe;
                if (_bufferTransferHandler != null) {
                    _bufferTransferHandler.transferData(this);
                }
            }
        }

        /* (non-Javadoc)
         * @see javax.media.protocol.PushBufferDataSource#getStreams()
         */
        @Override
        public PushBufferStream[] getStreams() {
            return new PushBufferStream[] {this};
        }

        /* (non-Javadoc)
         * @see javax.media.protocol.DataSource#getContentType()
         */
        @Override
        public String getContentType() {
            return _pbds.getContentType();
        }

        /* (non-Javadoc)
         * @see javax.media.protocol.DataSource#connect()
         */
        @Override
        public void connect() {
            // do nothing, base source is already connected
        }

        /* (non-Javadoc)
         * @see javax.media.protocol.DataSource#disconnect()
         */
        @Override
        public void disconnect() {
            stop();
        }

        /* (non-Javadoc)
         * @see javax.media.protocol.DataSource#start()
         */
        @Override
        public void start() {
            _started = true;
        }

        /* (non-Javadoc)
         * @see javax.media.protocol.DataSource#stop()
         */
        @Override
        public void stop() {
            _started = false;
        }

        /* (non-Javadoc)
         * @see javax.media.protocol.DataSource#getControl(java.lang.String)
         */
        @Override
        public Object getControl(String controlType) {
            System.out.println("getControl() request received: controlType=" + controlType);
            return _pbds.getControl(controlType);
        }

        /* (non-Javadoc)
         * @see javax.media.protocol.DataSource#getControls()
         */
        @Override
        public Object[] getControls() {
            //return EMPTY_CONTROLS_ARRAY;
            return _pbds.getControls();
        }

        /* (non-Javadoc)
         * @see javax.media.protocol.DataSource#getDuration()
         */
        @Override
        public Time getDuration() {
            return Duration.DURATION_UNKNOWN;
        }

        /* (non-Javadoc)
         * @see javax.media.protocol.PushBufferStream#getFormat()
         */
        public Format getFormat() {
            return _format;
        }

        /* (non-Javadoc)
         * @see javax.media.protocol.PushBufferStream#read(javax.media.Buffer)
         */
        public synchronized void read(Buffer buffer) throws IOException {
            if (_ioe != null) {
                throw _ioe;
            }
            buffer.copy(_buffer);
        }

        /* (non-Javadoc)
         * @see javax.media.protocol.PushBufferStream#setTransferHandler(javax.media.protocol.BufferTransferHandler)
         */
        public synchronized void setTransferHandler(BufferTransferHandler bufferTransferHandler) {
            _bufferTransferHandler = bufferTransferHandler;
        }

        /* (non-Javadoc)
         * @see javax.media.protocol.SourceStream#getContentDescriptor()
         */
        public ContentDescriptor getContentDescriptor() {
            return new ContentDescriptor(_pbds.getContentType());
        }

        /* (non-Javadoc)
         * @see javax.media.protocol.SourceStream#getContentLength()
         */
        public long getContentLength() {
            return LENGTH_UNKNOWN;
        }

        /* (non-Javadoc)
         * @see javax.media.protocol.SourceStream#endOfStream()
         */
        public synchronized boolean endOfStream() {
            return (_buffer != null && _buffer.isEOM());
        }

    }

}
