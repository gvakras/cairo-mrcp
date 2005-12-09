/*
 * Cairo - Open source framework for control of speech media resources.
 *
 * Copyright (C) 2005 Onomatopia, Inc. - http://www.onomatopia.com
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
package com.onomatopia.cairo.demo.util;

import com.onomatopia.cairo.server.rtp.RTPConsumer;
import com.onomatopia.cairo.server.tts.RTPPlayer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;

import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.Player;
import javax.media.protocol.PushBufferDataSource;
import javax.media.rtp.Participant;
import javax.media.rtp.ReceiveStream;

/**
 * TODOC
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 */
public class NativeMediaClient extends RTPConsumer {

    private static MediaLocator MICROPHONE = new MediaLocator("dsound://");

//    private static AudioFormat[] PREFERRED_MEDIA_FORMATS = {SourceAudioFormat.PREFERRED_MEDIA_FORMAT};
//    private static final ContentDescriptor CONTENT_DESCRIPTOR_RAW_RTP =
//        new ContentDescriptor(ContentDescriptor.RAW_RTP);

    private Player _player;
    private RTPPlayer _rtpPlayer;

    /**
     * TODOC
     * @param localPort 
     * @param remoteAddress 
     * @param remotePort 
     * @throws IOException 
     */
    public NativeMediaClient(int localPort, InetAddress remoteAddress, int remotePort)
      throws IOException {
        super(localPort, remoteAddress, remotePort);
        _rtpPlayer = new RTPPlayer(_rtpManager);
    }
    
    public void startTransmit() {
        new TransmitThread().start();
    }

    private class TransmitThread extends Thread {
        public void run() {
            try {
                _rtpPlayer.playSource(MICROPHONE);
            } catch (IllegalStateException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

//    private void createProcessor()
//      throws NoDataSourceException, IOException, NoProcessorException, CannotRealizeException {
//        MediaLocator locator = new MediaLocator("dsound://");
//        DataSource dataSource = Manager.createDataSource(locator);
//        ProcessorModel pm = new ProcessorModel(
//                dataSource, PREFERRED_MEDIA_FORMATS, CONTENT_DESCRIPTOR_RAW_RTP);
//        System.out.println("Creating realized processor...");
//        _processor = Manager.createRealizedProcessor(pm);
//
//    }


    /* (non-Javadoc)
     * @see com.onomatopia.cairo.server.rtp.RTPConsumer#streamReceived(javax.media.rtp.ReceiveStream, javax.media.protocol.PushBufferDataSource)
     */
    @Override
    public synchronized void streamReceived(ReceiveStream stream, PushBufferDataSource dataSource) {
        if (_player == null) {
            try {
//                System.out.println("Creating player for new stream...");
                _player = Manager.createRealizedPlayer(dataSource);
            } catch (Exception e) {
                System.out.println("Could not create player for new stream!");
                e.printStackTrace();
                return;
            }
//            System.out.println("Starting player...");
            _player.start();
        } else {
            System.out.println("Stream already received, ignoring new stream!");
        }
    }

    /* (non-Javadoc)
     * @see com.onomatopia.cairo.server.rtp.RTPConsumer#streamMapped(javax.media.rtp.ReceiveStream, javax.media.rtp.Participant)
     */
    @Override
    public void streamMapped(ReceiveStream stream, Participant participant) {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see com.onomatopia.cairo.server.rtp.RTPConsumer#streamInactive(javax.media.rtp.ReceiveStream, boolean)
     */
    @Override
    public void streamInactive(ReceiveStream stream, boolean byeEvent) {
        if (byeEvent) {
            synchronized (this) {
                // TODO: close player
            }
        }
    }

    /**
     * TODOC
     * @param args
     * @throws Exception 
     */
    public static void main(String[] args) throws Exception {
        int localPort = 42048;
        InetAddress remoteAddress = InetAddress.getLocalHost();
        int remotePort = 42050;

        NativeMediaClient client = new NativeMediaClient(localPort, remoteAddress, remotePort);
        client.startTransmit();

        BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Hit <enter> to shutdown...");
        String cmd = consoleReader.readLine();
        System.out.println("Shutting down...");
        client.shutdown();

    }

}
