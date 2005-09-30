package com.onomatopia.cairo.server.rtp;

import com.onomatopia.cairo.server.recog.sphinx.SourceAudioFormat;
import com.onomatopia.cairo.server.rtp.PBDSReplicator;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import javax.media.DataSink;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.Processor;
import javax.media.ProcessorModel;
import javax.media.datasink.DataSinkEvent;
import javax.media.datasink.DataSinkListener;
import javax.media.datasink.EndOfStreamEvent;
import javax.media.format.AudioFormat;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import javax.media.protocol.FileTypeDescriptor;
import javax.media.protocol.PushBufferDataSource;
import javax.media.rtp.Participant;
import javax.media.rtp.ReceiveStream;


/**
 * TODOC
 * @author Niels
 *
 */
public class RTPStreamReplicator extends RTPConsumer {

    private static AudioFormat[] PREFERRED_MEDIA_FORMATS = {SourceAudioFormat.PREFERRED_MEDIA_FORMAT};

    private static final ContentDescriptor CONTENT_DESCRIPTOR_RAW =
        new ContentDescriptor(ContentDescriptor.RAW);

    private PBDSReplicator _replicator;
    private Processor _processor;
    
    private int _port;

    public RTPStreamReplicator(int port) throws IOException {
        super(port);
        _port = port;
    }
    
    /**
     * TODOC
     * @return Returns the port.
     */
    public int getPort() {
        return _port;
    }

    /* (non-Javadoc)
     * @see com.onomatopia.cairo.server.rtp.RTPConsumer#shutdown()
     */
    @Override
    public void shutdown() {
        super.shutdown();
    }

    /* (non-Javadoc)
     * @see com.onomatopia.cairo.server.rtp.RTPConsumer#streamReceived(javax.media.rtp.ReceiveStream, javax.media.protocol.PushBufferDataSource)
     */
    @Override
    public synchronized void streamReceived(ReceiveStream stream, PushBufferDataSource dataSource) {
        if (_replicator == null) {
            try {
                ProcessorModel pm = new ProcessorModel(
                        dataSource, PREFERRED_MEDIA_FORMATS, CONTENT_DESCRIPTOR_RAW);
                try {
                    System.out.println("Creating realized processor...");
                    _processor = Manager.createRealizedProcessor(pm);
                } catch (IOException e){
                    throw e;
                } catch (javax.media.CannotRealizeException e){
                    throw (IOException) new IOException(e.getMessage()).initCause(e);
                } catch (javax.media.NoProcessorException e){
                    throw (IOException) new IOException(e.getMessage()).initCause(e);
                }

                System.out.println("Processor realized.");

                PushBufferDataSource pbds = (PushBufferDataSource) _processor.getDataOutput();
                _replicator = new PBDSReplicator(pbds);
                _processor.start();
                this.notifyAll();
            } catch (IOException e) {
                _processor = null;
                _replicator = null;  // TODO: close properly
                e.printStackTrace();
            }
        }
    }

    /* (non-Javadoc)
     * @see com.onomatopia.cairo.server.rtp.RTPConsumer#streamMapped(javax.media.rtp.ReceiveStream, javax.media.rtp.Participant)
     */
    @Override
    public void streamMapped(ReceiveStream stream, Participant participant) {
        // ignore
    }

    /* (non-Javadoc)
     * @see com.onomatopia.cairo.server.rtp.RTPConsumer#streamInactive(javax.media.rtp.ReceiveStream, boolean)
     */
    @Override
    public synchronized void streamInactive(ReceiveStream stream, boolean byeEvent) {
        //if (byeEvent) {
            _replicator = null; // TODO: close data source properly, make sure this triggers EndOfStreamEvent in replicated PBDS
            if (_processor != null) {
                System.out.println("Closing RTP processor for SSRC=" + stream.getSSRC());
                _processor.close();
                _processor = null;
            }
        //}
    }


    /**
     * TODOC
     * @param outputContentDescriptor A <code>ContentDescriptor</code> that describes the desired output content-type.
     * @return A new <code>Processor</code> that is in the <code>Realized</code> state.
     * @throws IOException
     * @throws IllegalStateException
     */
    public synchronized Processor createRealizedProcessor(ContentDescriptor outputContentDescriptor)
      throws IOException, IllegalStateException {
        return createRealizedProcessor(outputContentDescriptor, -1);
    }

    /**
     * TODOC
     * @param outputContentDescriptor A <code>ContentDescriptor</code> that describes the desired output content-type.
     * @param timeout the maximum time to wait in milliseconds
     * @return A new <code>Processor</code> that is in the <code>Realized</code> state.
     * @throws IOException
     * @throws IllegalStateException
     */
    public synchronized Processor createRealizedProcessor(ContentDescriptor outputContentDescriptor, long timeout)
      throws IOException, IllegalStateException {

        if (_replicator == null) {
            if (timeout >= 0) {
                try {
                    this.wait(timeout);
                } catch (InterruptedException e) {
                    // TODO: throw this exception?
                    e.printStackTrace();
                }
            }
            if (_replicator == null) {
                throw new IllegalStateException("No RTP stream yet received!");
            }
        }


        ProcessorModel pm = new ProcessorModel(
            _replicator.replicate(), PREFERRED_MEDIA_FORMATS, outputContentDescriptor);
        Processor processor;
        try {
            System.out.println("Creating realized processor...");
            processor = Manager.createRealizedProcessor(pm);
        } catch (IOException e){
            throw e;
        } catch (javax.media.CannotRealizeException e){
            throw (IOException) new IOException(e.getMessage()).initCause(e);
        } catch (javax.media.NoProcessorException e){
            throw (IOException) new IOException(e.getMessage()).initCause(e);
        }

        System.out.println("Processor realized.");
        
        return processor;

    }


}
