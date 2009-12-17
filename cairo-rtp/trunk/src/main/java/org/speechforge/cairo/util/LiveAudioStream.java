package org.speechforge.cairo.util;

import javax.media.*;
import javax.media.format.*;
import javax.media.protocol.*;

import org.apache.log4j.Logger;


import java.io.IOException;
import java.io.InputStream;
 
public class LiveAudioStream implements PushBufferStream, Runnable {
	
    private static Logger _logger = Logger.getLogger(LiveAudioStream.class);
    
	
    protected ContentDescriptor cd = new ContentDescriptor(ContentDescriptor.RAW);

	    protected byte [] data;
	    protected AudioFormat audioFormat;
	    protected boolean started;
	    protected Thread thread;
	    protected BufferTransferHandler transferHandler;
	    protected Control [] controls = new Control[0];
	    
		private InputStream is;
		
		int MAXSIZE=1000;

        //byte[] buff = new byte[MAXSIZE];
	    
	    public LiveAudioStream(InputStream is, AudioFormat format) {
		
	    	this.is = is;
	    	audioFormat = format;
	

		thread = new Thread(this);
	    }

	    /***************************************************************************
	     * SourceStream
	     ***************************************************************************/
	    
	    public ContentDescriptor getContentDescriptor() {
		return cd;
	    }

	    public long getContentLength() {
		return LENGTH_UNKNOWN;
	    }

	    public boolean endOfStream() {
		return false;
	    }

	    /***************************************************************************
	     * PushBufferStream
	     ***************************************************************************/

	    int seqNo = 0;
	    long timeStamp = System.currentTimeMillis();
	    
	    public Format getFormat() {
		    return audioFormat;
	    }

	    public void read(Buffer buffer) throws IOException {
	    	synchronized (this) {
	    		Object outdata = buffer.getData();
	    		if (outdata == null || !(outdata.getClass() == Format.byteArray) ||
	    				((byte[])outdata).length < MAXSIZE) {
	    			outdata = new byte[MAXSIZE];
	    			buffer.setData(outdata);
	    		}
	            byte[] buff = (byte[])buffer.getData();
	            
	    		int offset = 0;
	    		int bytesToRead = MAXSIZE;
	    		int totalRead = 0;
	    		int count = 0;
	    		while (totalRead < MAXSIZE) {
				   int size = is.read(buff,offset,bytesToRead);
				   totalRead = totalRead + size;
				   offset = offset + size-1;
				   bytesToRead = bytesToRead - size;
				   count++;
				}
				buffer.setData(buff);
				_logger.debug("bytes read from stream: "+totalRead+" "+count);

	    		
    			buffer.setFormat( audioFormat );
    			buffer.setTimeStamp(timeStamp);
	    		buffer.setSequenceNumber( seqNo );
	    		buffer.setLength(totalRead);
	    		buffer.setFlags(0);
	    		buffer.setHeader( null );
	    		seqNo++;
	    		timeStamp= (long) (timeStamp + (8*totalRead/audioFormat.getSampleSizeInBits()/audioFormat.getSampleRate() ));
	    	}
	    }

	    public void setTransferHandler(BufferTransferHandler transferHandler) {
	    	synchronized (this) {
	    		this.transferHandler = transferHandler;
	    		notifyAll();
	    	}
	    }

	    void start(boolean started) {
	    	synchronized ( this ) {
	    		this.started = started;
	    		if (started && !thread.isAlive()) {
	    			thread = new Thread(this);
	    			thread.start();
	    		}
	    		notifyAll();
	    	}
	    }

	    /***************************************************************************
	     * Runnable
	     ***************************************************************************/

	    public void run() {
	    	while (started) {
	    		synchronized (this) {
	    			while (transferHandler == null && started) {
	    				try {
	    					wait(1000);
	    				} catch (InterruptedException ie) {
	    				}
	    			} // while
	    		}

	    		if (started && transferHandler != null) {
	    			transferHandler.transferData(this);
	    			try {
	    				Thread.currentThread().sleep( 10 );
	    			} catch (InterruptedException ise) {
	    			}
	    		}
	    	} // while (started)
	    } // run

	    // Controls
	    
	    public Object [] getControls() {
	    	return controls;
	    }

	    public Object getControl(String controlType) {
	       try {
	          Class  cls = Class.forName(controlType);
	          Object cs[] = getControls();
	          for (int i = 0; i < cs.length; i++) {
	             if (cls.isInstance(cs[i]))
	                return cs[i];
	          }
	          return null;

	       } catch (Exception e) {   // no such controlType or such control
	         return null;
	       }
	    }
	}
