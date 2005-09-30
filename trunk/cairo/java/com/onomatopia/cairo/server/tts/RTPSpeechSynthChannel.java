/**
 * TODOC
 * 
 */
package com.onomatopia.cairo.server.tts;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;

import javax.media.Format;
import javax.media.Manager;
import javax.media.Processor;
import javax.media.control.TrackControl;
import javax.media.format.UnsupportedFormatException;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import javax.media.rtp.InvalidSessionAddressException;
import javax.media.rtp.RTPManager;
import javax.media.rtp.SessionAddress;

/**
 * TODOC
 * @author Niels
 *
 */
public class RTPSpeechSynthChannel {

    static final ContentDescriptor CONTENT_DESCRIPTOR_RAW_RTP = new ContentDescriptor(ContentDescriptor.RAW_RTP);
    //private static final AudioFormat[] PREFERRED_MEDIA_FORMATS = {SourceAudioFormat.PREFERRED_MEDIA_FORMAT};

    static final short IDLE = 0;
    static final short SPEAKING = 1;
    static final short PAUSED = 2;

    volatile short _state = IDLE;

    BlockingQueue<File> _promptQueue = new LinkedBlockingQueue<File>();
    //RTPManager _rtpManager;
    private Thread _sendThread;
    RTPPlayer _promptPlayer;
    private int _localPort;
    private InetAddress _remoteAddress;
    private int _remotePort;

    /**
     * TODOC
     * @param localPort 
     * @param remoteAddress 
     * @param remotePort 
     */
    public RTPSpeechSynthChannel(int localPort, InetAddress remoteAddress, int remotePort) {
        _localPort = localPort;
        _remoteAddress = remoteAddress;
        _remotePort = remotePort;
    }

    // TODO: move to config file
    private static final File SILENCE_PROMPT_FILE = new File("C:\\work\\cvs\\onomatopia\\cairo\\prompts\\silence250.wav");

    private synchronized void init() throws InvalidSessionAddressException, IOException {
        if (_promptPlayer == null) {
            _promptPlayer = new RTPPlayer(_localPort, _remoteAddress, _remotePort);

            try {
                _promptQueue.put(SILENCE_PROMPT_FILE);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            (_sendThread = new SendThread()).start();

            /*System.out.println("init(): Waiting 5 seconds for prompt player to \"settle\"...");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }*/
        }
    }

    public synchronized int queuePrompt(File promptFile) throws InvalidSessionAddressException, IOException {
        init();
        int state = _state;
        try {
            _promptQueue.put(promptFile);
            _state = SPEAKING;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return state;
    }
    
    public synchronized void stopPlayback() {
        _sendThread.interrupt();
    }

    private class SendThread extends Thread {
        
        volatile boolean _run = true;
        
//        @Override
//        public synchronized void interrupt() {
//            super.interrupt();
//        }
//        
//        @Override
//        public synchronized boolean isInterrupted() {
//            return super.isInterrupted();
//        }

        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
            /*if (Thread.currentThread() != this) {
                throw new RuntimeException();
            }*/
            while (_run) {
                boolean drainQueue = false;
                try {

                    File promptFile = _promptQueue.take();
                    _promptPlayer.playPrompt(promptFile);

                    drainQueue = Thread.interrupted();

                } catch (InterruptedException e) {
                    e.printStackTrace();
                    // TODO: cancel current prompt playback
                    drainQueue = true;

                } catch (Exception e) {
                    e.printStackTrace();
                    //skip and try next prompt...
                }

                if (drainQueue) {
                    while (!_promptQueue.isEmpty()) {
                        try {
                            _promptQueue.take();
                            //TODO: may need to remove only specific prompts
                            // (e.g. save and put back in queue if not in cancel list)
                        } catch (InterruptedException e1) {
                            // should not happen since this is the only thread consuming from queue
                            e1.printStackTrace();
                        }
                    }
                }
                _state = _promptQueue.isEmpty() ? IDLE : SPEAKING;
            }
        }
    }

    /**
     * TODOC
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        
        File promptDir = new File("C:\\work\\cvs\\onomatopia\\cairo\\prompts\\test");

        int localPort = 42050;
        InetAddress remoteAddress = InetAddress.getLocalHost();
        int remotePort = 42048;

        RTPSpeechSynthChannel player = new RTPSpeechSynthChannel(localPort, remoteAddress, remotePort);
        
        File prompt = new File(promptDir, "good_morning_rita.wav");
        player.queuePrompt(prompt);
        player.queuePrompt(prompt);
        player.queuePrompt(prompt);
        player.queuePrompt(prompt);
        player.queuePrompt(prompt);
    }

}
