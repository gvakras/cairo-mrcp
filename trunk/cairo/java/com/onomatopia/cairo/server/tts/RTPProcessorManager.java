/**
 * TODOC
 * 
 */
package com.onomatopia.cairo.server.tts;

import java.io.IOException;

import javax.media.CannotRealizeException;
import javax.media.Controller;
import javax.media.ControllerClosedEvent;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.EndOfMediaEvent;
import javax.media.Processor;
import javax.media.format.UnsupportedFormatException;
import javax.media.protocol.DataSource;
import javax.media.rtp.RTPManager;
import javax.media.rtp.SendStream;

/**
 * TODOC
 * @author Niels
 *
 */
@Deprecated public class RTPProcessorManager implements ControllerListener {

    private /*volatile*/ boolean closed = false;

    private RTPManager _rtpManager;
    private Processor _processor;

    /**
     * TODOC
     * @param manager 
     * @param processor 
     */
    public RTPProcessorManager(RTPManager manager, Processor processor) {
        _rtpManager = manager;
        _processor = processor;
        _processor.addControllerListener(this);
    }

    public synchronized void configure() throws IOException, InterruptedException {
        _processor.configure();
        while(_processor.getState() < Processor.Configured  && !closed) {
            this.wait();
        }
        if (closed) {
            throw new IOException("Processor closed unexpectedly!");
        }
    }

    public synchronized void realize() throws CannotRealizeException, InterruptedException {
       _processor.realize();
       while(_processor.getState() < Controller.Realized  && !closed) {
           this.wait();
       }
       if (closed) {
           throw new CannotRealizeException("Processor closed unexpectedly!");
       }
    }

    public synchronized void play() throws UnsupportedFormatException, IOException, InterruptedException {
        InterruptedException ie = null;

        DataSource dataOutput = _processor.getDataOutput();
        SendStream sendStream = _rtpManager.createSendStream(dataOutput, 0);
        sendStream.start();
        _processor.start();

        while(!closed) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                if (ie == null) {
                    ie = e;
                    System.out.println("play(): encountered interrupt while waiting for prompt to complete, stopping playback prematurely...");
                    _processor.close();
                } else {
                    System.out.println("play(): encountered double interrupt, returning without waiting for ControllerClosedEvent...");
                    ie.printStackTrace();
                    throw e;
                }
            }
        }
        
        if (ie != null) {
            throw ie;
        }

    }

    /* (non-Javadoc)
     * @see javax.media.ControllerListener#controllerUpdate(javax.media.ControllerEvent)
     */
    public synchronized void controllerUpdate(ControllerEvent event) {
        System.out.println("ControllerEvent received: " + event);

        if (event instanceof EndOfMediaEvent) {
            _processor.close();
        } else if (event instanceof ControllerClosedEvent) {
            closed = true;
        }

        this.notifyAll();
    }

}
