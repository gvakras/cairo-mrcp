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
package  org.speechforge.cairo.rtp;

import static org.speechforge.cairo.jmf.JMFUtil.CONTENT_DESCRIPTOR_RAW_RTP;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.util.Enumeration;
import java.util.Vector;

import javax.media.CannotRealizeException;
import javax.media.Codec;
import javax.media.Controller;
import javax.media.ControllerClosedEvent;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.EndOfMediaEvent;
import javax.media.Format;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.PackageManager;
import javax.media.Processor;
import javax.media.UnsupportedPlugInException;
import javax.media.control.PacketSizeControl;
import javax.media.control.TrackControl;
import javax.media.format.UnsupportedFormatException;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import javax.media.rtp.InvalidSessionAddressException;
import javax.media.rtp.RTPManager;
import javax.media.rtp.SendStream;
import javax.media.rtp.SessionAddress;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.apache.log4j.Logger;
import org.jlibrtp.DataFrame;
import org.jlibrtp.Participant;
import org.jlibrtp.RTPAppIntf;
import org.jlibrtp.RTPSession;
//import org.speechforge.cairo.util.sip.AudioFormats;

/**
 * Handles playing of audio prompt files over an RTP output stream.
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 */
public class RTPPlayerJlibRtpImpl implements  RTPAppIntf {

    private static Logger _logger = Logger.getLogger(RTPPlayerJlibRtpImpl.class);

    private Object _lock = new Object();
 

    private final int EXTERNAL_BUFFER_SIZE = 1024;
    static int pktCount = 0;
    static int dataCount = 0;
    
    int localPort;
    InetAddress remoteAddress;
    int remotePort;
    AudioFormats af;

	private RTPSession rtpSession;

    public RTPPlayerJlibRtpImpl(int localPort, InetAddress remoteAddress, int remotePort, AudioFormats af)
      throws InvalidSessionAddressException, IOException {

    	this.localPort = localPort;
    	this.remoteAddress = remoteAddress;
    	this.remotePort = remotePort;
    	this.af = af;

    	DatagramSocket rtpSocket = null;
    	DatagramSocket rtcpSocket = null;

    	try {
    		rtpSocket = new DatagramSocket(localPort);
    		rtcpSocket = new DatagramSocket(localPort+1);
    	} catch (Exception e) {
    		System.out.println("RTPSession failed to obtain port");
    	}

    	rtpSession = new RTPSession(rtpSocket, rtcpSocket);
    	rtpSession.registerRTPSession(this,null, null);
    	System.out.println("CNAME: " + rtpSession.CNAME());
    	

    	Participant p = new Participant(remoteAddress.getHostAddress(),remotePort,remotePort + 1);
        rtpSession.addParticipant(p);

    }

    public void playPrompt(File soundFile) throws InterruptedException, IllegalStateException, IllegalArgumentException {


    	if (!soundFile.exists()) {
    		throw new IllegalArgumentException("Specified prompt file does not exist: " + soundFile);
    	}

    	AudioInputStream audioInputStream = null;
    	try {
    		audioInputStream = AudioSystem.getAudioInputStream(soundFile);
    	} catch (UnsupportedAudioFileException e1) {
    		e1.printStackTrace();
    		return;
    	} catch (IOException e1) {
    		e1.printStackTrace();
    		return;
    	}

    	//AudioFormat format = audioInputStream.getFormat();
    	AudioFormat.Encoding encoding =  new AudioFormat.Encoding("PCM_SIGNED");
    	AudioFormat format = new AudioFormat(encoding,((float) 16000.0), 16, 1, 2, ((float) 16000.0) ,false);
    	System.out.println(format.toString());


    	/*if(! this.local) {
            // To time the output correctly, we also play at the input:
            auline = null;
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

            try {
                auline = (SourceDataLine) AudioSystem.getLine(info);
                auline.open(format);
            } catch (LineUnavailableException e) {
                e.printStackTrace();
                return;
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

            if (auline.isControlSupported(FloatControl.Type.PAN)) {
                FloatControl pan = (FloatControl) auline
                .getControl(FloatControl.Type.PAN);
                if (this.curPosition == Position.RIGHT)
                    pan.setValue(1.0f);
                else if (this.curPosition == Position.LEFT)
                    pan.setValue(-1.0f);
            }

            auline.start();
    	}*/

    	int nBytesRead = 0;
    	byte[] abData = new byte[EXTERNAL_BUFFER_SIZE];
    	long start = System.currentTimeMillis();
    	try {
    		while (nBytesRead != -1 && pktCount < 200) {
    			nBytesRead = audioInputStream.read(abData, 0, abData.length);

    			if (nBytesRead >= 0) {
    				rtpSession.sendData(abData);
    				//if(!this.local) {
    					//auline.write(abData, 0, abData.length);

    					//dataCount += abData.length;

    					//if(pktCount % 10 == 0) {
    					//	System.out.println("pktCount:" + pktCount + " dataCount:" + dataCount);
    					//
    					//	long test = 0;
    					//	for(int i=0; i<abData.length; i++) {
    					//		test += abData[i];
    					//	}
    					//	System.out.println(Long.toString(test));
    					//}

    					pktCount++;
    					//if(pktCount == 100) {
    					//	System.out.println("Time!!!!!!!!! " + Long.toString(System.currentTimeMillis()));
    					//}
    					//System.out.println("yep");
    			}
    			if(pktCount == 100) {
    				Enumeration<Participant> iter = this.rtpSession.getParticipants();
    				//System.out.println("iter " + iter.hasMoreElements());
    				Participant p = null;

    				while(iter.hasMoreElements()) {
    					p = iter.nextElement();

    					String name = "name";
    					byte[] nameBytes = name.getBytes();
    					String data= "abcd";
    					byte[] dataBytes = data.getBytes();


    					int ret = rtpSession.sendRTCPAppPacket(p.getSSRC(), 0, nameBytes, dataBytes);
    					System.out.println("!!!!!!!!!!!! ADDED APPLICATION SPECIFIC " + ret);
    					continue;
    				}
    				if(p == null)
    					System.out.println("No participant with SSRC available :(");
    			}
    		}
    	} catch (IOException e) {
    		e.printStackTrace();
    		return;
    	}
    	System.out.println("Time: " + (System.currentTimeMillis() - start)/1000 + " s");

    	try { Thread.sleep(200);} catch(Exception e) {}

    	this.rtpSession.endSession();

    	try { Thread.sleep(2000);} catch(Exception e) {}
    }





    public void playSource(MediaLocator source) throws InterruptedException, IllegalStateException {
    	
    }

    
    public void playStream(InputStream stream) throws InterruptedException, IllegalStateException {


 
                /*
                 AudioInputStream audioInputStream = null;
    	    	//AudioFormat format = audioInputStream.getFormat();
    	    	AudioFormat.Encoding encoding =  new AudioFormat.Encoding("PCM_SIGNED");
    	    	AudioFormat format = new AudioFormat(encoding,((float) 8000.0), 16, 1, 2, ((float) 8000.0) ,false);
    	    	System.out.println(format.toString());
    	    	
    	    	*/

    	    	/*if(! this.local) {
    	            // To time the output correctly, we also play at the input:
    	            auline = null;
    	            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

    	            try {
    	                auline = (SourceDataLine) AudioSystem.getLine(info);
    	                auline.open(format);
    	            } catch (LineUnavailableException e) {
    	                e.printStackTrace();
    	                return;
    	            } catch (Exception e) {
    	                e.printStackTrace();
    	                return;
    	            }

    	            if (auline.isControlSupported(FloatControl.Type.PAN)) {
    	                FloatControl pan = (FloatControl) auline
    	                .getControl(FloatControl.Type.PAN);
    	                if (this.curPosition == Position.RIGHT)
    	                    pan.setValue(1.0f);
    	                else if (this.curPosition == Position.LEFT)
    	                    pan.setValue(-1.0f);
    	            }

    	            auline.start();
    	    	}*/

    			long bytesPerSample = 2;
    			long sampleRate = 8000;
	    	    AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,((float) 8000.0), 16, 1, 2, ((float) 4000.0) ,true); 
    			AudioInputStream ais = new AudioInputStream(stream,format,-1);
	            //ais = AudioSystem.getAudioInputStream(stream,format,-1);
        
     			AudioInputStream aisUlaw;
    			aisUlaw = AudioSystem.getAudioInputStream(AudioFormat.Encoding.ULAW,ais);

    			long timestamp = 0;
    			long seq=0;
    	
    	    	int nBytesRead = 0;
    	    	byte[] abData = new byte[EXTERNAL_BUFFER_SIZE];
    	    	long start = System.currentTimeMillis();
    	    	try {
    	    		while (nBytesRead != -1 && pktCount < 200) {
    	    			nBytesRead = aisUlaw.read(abData, 0, abData.length);

    	    			if (nBytesRead >= 0) {
    	    				timestamp = timestamp +((1000*nBytesRead*bytesPerSample)/sampleRate);
    	    				System.out.println(nBytesRead+"  "+timestamp+"  "+seq+ " " +abData.length);
    	    				rtpSession.sendData(abData,timestamp,seq++);
    	    				//if(!this.local) {
    	    					//auline.write(abData, 0, abData.length);

    	    					dataCount += abData.length;

    	    					if(pktCount % 10 == 0) {
    	    						System.out.println("pktCount:" + pktCount + " dataCount:" + dataCount);
    	    					
    	    						//long test = 0;
    	    						//for(int i=0; i<abData.length; i++) {
    	    						//	test += abData[i];
    	    						//}
    	    						//System.out.println(Long.toString(test));
    	    					}

    	    					pktCount++;
    	    					//if(pktCount == 100) {
    	    					//	System.out.println("Time!!!!!!!!! " + Long.toString(System.currentTimeMillis()));
    	    					//}
    	    					//System.out.println("yep");
    	    			}
    	    			if(pktCount == 100) {
    	    				Enumeration<Participant> iter = this.rtpSession.getParticipants();
    	    				//System.out.println("iter " + iter.hasMoreElements());
    	    				Participant p = null;

    	    				while(iter.hasMoreElements()) {
    	    					p = iter.nextElement();

    	    					String name = "name";
    	    					byte[] nameBytes = name.getBytes();
    	    					String data= "abcd";
    	    					byte[] dataBytes = data.getBytes();


    	    					int ret = rtpSession.sendRTCPAppPacket(p.getSSRC(), 0, nameBytes, dataBytes);
    	    					System.out.println("!!!!!!!!!!!! ADDED APPLICATION SPECIFIC " + ret);
    	    					continue;
    	    				}
    	    				if(p == null)
    	    					System.out.println("No participant with SSRC available :(");
    	    			}
    	    		}
    	    	} catch (IOException e) {
    	    		e.printStackTrace();
    	    		return;
    	    	}
    	    	System.out.println("Time: " + (System.currentTimeMillis() - start)/1000 + " s");





    	    }	


    
    private void checkInterrupted() throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
    }









    
    private void close() throws InterruptedException {
    	_logger.info("Called close");

    	this.rtpSession.endSession();

    }

   

    public void shutdown() {
     
        try {
            this.close();
        } catch (InterruptedException e) {
            _logger.warn("Interrupted while closing rtp processor, exception message: "+e.getLocalizedMessage());
        }
        
        /* Some of the possible methods that may be needed for shutting down the rp player
         * All that seems to be needed, is to close the processor.  The RTPManager gets shutdown by the RTPConsumer
         * (RTPManager is shared with this class and the NativeMediaClient which is a subclass of the RTPCOnsumer).
         * 
         */
 
           

    }




	public int frameSize(int arg0) {
	    // TODO Auto-generated method stub
	    return 0;
    }




	public void receiveData(DataFrame arg0, Participant arg1) {
	    // TODO Auto-generated method stub
	    
    }




	public void userEvent(int arg0, Participant[] arg1) {
	    // TODO Auto-generated method stub
	    
    }
}
