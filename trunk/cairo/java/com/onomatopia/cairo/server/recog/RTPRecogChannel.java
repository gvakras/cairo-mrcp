/**
 * TODOC
 * 
 */
package com.onomatopia.cairo.server.recog;

import com.onomatopia.cairo.server.recog.sphinx.SphinxRecEngine;
import com.onomatopia.cairo.server.resource.ResourceUnavailableException;
import com.onomatopia.cairo.server.rtp.RTPStreamReplicator;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.TimeoutException;

import javax.media.ConfigureCompleteEvent;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.Processor;
import javax.media.RealizeCompleteEvent;
import javax.media.StartEvent;
import javax.media.control.TrackControl;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import javax.media.protocol.FileTypeDescriptor;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;

import org.apache.commons.lang.Validate;
import org.apache.commons.pool.ObjectPool;
import org.mrcp4j.MrcpEventName;
import org.mrcp4j.MrcpRequestState;
import org.mrcp4j.message.MrcpEvent;
import org.mrcp4j.server.MrcpSession;

/**
 * TODOC
 * @author Niels
 *
 */
public class RTPRecogChannel implements ControllerListener {

    private static final ContentDescriptor CONTENT_DESCRIPTOR_RAW =
        new ContentDescriptor(ContentDescriptor.RAW);

    ObjectPool _recEnginePool;
    private RTPStreamReplicator _replicator;

    private Processor _processor;

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
     * @param startInputTimers 
     * @param grammarName 
     * @throws IOException 
     * @throws IllegalStateException 
     * @throws ResourceUnavailableException 
     */
    public void recognize(RecogListener listener, boolean startInputTimers, GrammarLocation grammarLocation) throws IllegalStateException, IOException, ResourceUnavailableException {
        if (_processor != null) {
            throw new IllegalStateException("Recognition already in progress!");
        }
        
        _processor = _replicator.createRealizedProcessor(CONTENT_DESCRIPTOR_RAW, 10000); // TODO: specify audio format

        PushBufferDataSource dataSource = (PushBufferDataSource) _processor.getDataOutput();
        if (dataSource == null) {
            throw new IOException("Processor.getDataOutput() returned null!");
        }
        
        SphinxRecEngine recEngine = null;
        try {
            recEngine = (SphinxRecEngine) _recEnginePool.borrowObject();
        } catch (Exception e) {
            e.printStackTrace();
            _processor.close();
            _processor = null;
            throw new ResourceUnavailableException("All rec engines are in use!", e);
            // TODO: wait for availability...?
        }
        
        try {
            System.out.println("Starting recognition...");
            recEngine.loadJSGF(grammarLocation);
            recEngine.startRecognition(dataSource);
            _processor.addControllerListener(this);
            _processor.start();
            //dataSource.connect();
            //dataSource.start();
            // TODO: hang on to recEngine for recognition completion, etc.
            new RecogThread(recEngine, listener).start();
        } catch (IOException e) {
            _processor.close();
            _processor = null;
            try {
                _recEnginePool.returnObject(recEngine);
            } catch (Exception e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            throw e;
        }
        
    }
    
    private class RecogThread extends Thread {

        private SphinxRecEngine _recEngine;
        private RecogListener _listener;

        private RecogThread(SphinxRecEngine recEngine, RecogListener listener) {
            _recEngine = recEngine;
            _listener = listener;
        }
        
        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
            System.out.println("RecogThread waiting for result...");
            RecognitionResult result = _recEngine.waitForResult(_listener);
            System.out.println("**************************************************************");
            System.out.println("RecogThread got result: " + result.toString());
            System.out.println("**************************************************************");
            try {
                _recEnginePool.returnObject(_recEngine);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            _listener.recognitionComplete();
        }
    }

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
