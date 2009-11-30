package org.speechforge.cairo.client.cloudimpl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.media.rtp.InvalidSessionAddressException;
import javax.sound.sampled.AudioFormat;


import org.apache.log4j.Logger;
import org.mrcp4j.MrcpEventName;
import org.mrcp4j.MrcpRequestState;
import org.mrcp4j.client.MrcpInvocationException;
import org.mrcp4j.message.MrcpEvent;
import org.mrcp4j.message.header.CompletionCause;
import org.mrcp4j.message.header.IllegalValueException;
import org.mrcp4j.message.header.MrcpHeader;
import org.mrcp4j.message.header.MrcpHeaderName;
import org.speechforge.cairo.client.NoMediaControlChannelException;
import org.speechforge.cairo.client.SpeechClient;
import org.speechforge.cairo.client.SpeechClientProvider;
import org.speechforge.cairo.client.SpeechEventListener;
import org.speechforge.cairo.client.SpeechRequest;
import org.speechforge.cairo.client.SpeechRequest.RequestType;
import org.speechforge.cairo.client.recog.InvalidRecogResultException;
import org.speechforge.cairo.client.recog.RecognitionResult;
import org.speechforge.cairo.rtp.server.RTPStreamReplicator;

import com.spokentech.speechdown.client.HttpRecognizer;
import com.spokentech.speechdown.client.HttpSynthesizer;
import com.spokentech.speechdown.client.PromptPlayListener;
import com.spokentech.speechdown.client.endpoint.RtpS4EndPointingInputStream;
import com.spokentech.speechdown.client.rtp.RtpTransmitter;
//TODO: Remove the dependency on MRCP4j (state and two exceptions)


public class SpeechCloudClient implements SpeechClient, SpeechClientProvider, PromptPlayListener {


	/** The _logger. */
    private static Logger _logger = Logger.getLogger(SpeechCloudClient.class);
 
    
    private static final String wav = "audio/x-wav";
    private static final String mpeg = "audio/mpeg";
    
    private static final String recServiceUrl = "http://ec2-174-129-20-250.compute-1.amazonaws.com/speechcloud/SpeechUploadServlet";    
    private static final String synthServiceUrl = "http://ec2-174-129-20-250.compute-1.amazonaws.com/speechcloud/SpeechDownloadServlet";    

    private static final  String s4audio = "audio/x-s4audio";
    
    /** The _barge in flag */
    private boolean _bargeIn = false;

    /**
     * The Enum DtmfState.
     */
    
    public enum DtmfState {
          notActive, 
          waitingForInput, 
          waitingForMatch, 
          complete}
    
    /** The _dtmf state. */
    private DtmfState _dtmfState = DtmfState.notActive; 
    
    /** The _timer. */
    private /*static*/ Timer _timer = new Timer();
    
    /** The _no input timeout task. */
    TimerTask _noInputTimeoutTask;
    
    /** The _no recog timeout task. */
    TimerTask _noRecogTimeoutTask;
    
    /** pattern to be matched for dtmf recognition (regex) */
    private Pattern _pattern;
    
    /** the listener set in the dtmf requests (for dtmf recognition events) */
    private SpeechEventListener _dtmfListener;
    
    /** dtmf no recognition timeout value */
    long _recogTimout;

    /** dtmf input buffer. the string to be matched against the pattern in the characterEventReceived 
     *  method when there is a match call the listener and this dtmf request is completed */
    String _inBuf;
    char[] _charArray;
    int _length = 0;
    String sal;
    
    String tempDir = "c:/temp/";
    
    boolean lmflg = false; 
    boolean batchFlag = true;
    int timeout = 0;
    
    
    private String voiceName = "default";
	//voice = "hmm-jmk";
	//voice = "jmk-arctic";
	//voice = "hmm-slt";
	//voice = "slt-arctic";
	 //voice = "hmm-bdl";
	 //voice = "bdl-arctic";
	 //voice = "misspelled";

    /**
     * @return the voiceName
     */
    public String getVoiceName() {
    	return voiceName;
    }


	/**
     * @param voiceName the voiceName to set
     */
    public void setVoiceName(String voiceName) {
    	this.voiceName = voiceName;
    }


	/**
     * @return the tempDir
     */
    public String getTempDir() {
    	return tempDir;
    }


	/**
     * @param tempDir the tempDir to set
     */
    public void setTempDir(String tempDir) {
    	this.tempDir = tempDir;
    }

	private  Collection<SpeechEventListener> listenerList = null;
	
 
    private String serviceUrl = null;
    
    private RTPStreamReplicator rtpReplicator;
    private HttpRecognizer recognizer;
    
    private RtpTransmitter rtpTransmitter;
    private HttpSynthesizer synthesizer;
    
    
    RequestType _activeRequestType;
    SpeechRequest _activeBlockingTts;
    
    /** The _active recognition. */
    SpeechRequest _activeRecognition;

    
    
    public SpeechCloudClient(RTPStreamReplicator rtpReplicator, RtpTransmitter rtpTransmitter) {
        super();
     
        this.rtpReplicator = rtpReplicator;
        this.rtpTransmitter = rtpTransmitter;  
        
        synthesizer = new HttpSynthesizer();
        if (serviceUrl != null)
           synthesizer.setService(serviceUrl+"/SpeechDownloadServlet");
        else
            synthesizer.setService(synthServiceUrl);
        
        recognizer = new HttpRecognizer();
        if (serviceUrl != null)
           recognizer.setService(serviceUrl+"/SpeechUploadServlet");
        else
           recognizer.setService(recServiceUrl);
        
        listenerList = new java.util.ArrayList<SpeechEventListener>();   
    }
    

    /**
     * @return the serviceUrl
     */
    public String getServiceUrl() {
    	return serviceUrl;
    }


	/**
     * @param serviceUrl the serviceUrl to set
     */
    public void setServiceUrl(String serviceUrl) {
    	this.serviceUrl = serviceUrl;
    }


	/* (non-Javadoc)
     * @see org.speechforge.cairo.client.SpeechClientProvider#characterEventReceived(char)
     */
    public void characterEventReceived(char c) {
        _logger.debug("speechclient.chareventreceived: "+c);
        
        if (_dtmfState == DtmfState.waitingForInput) {
            _logger.debug("   waitingfor input...");
            //if the first char, 
            //  1.  cancel the no input timer  and
            //  2.  start the no recognition timer and 
            //  3.  cancel the speech recognition
            
            //cancel the no input timer
            if (_noInputTimeoutTask != null) {
                _noInputTimeoutTask.cancel();
                _noInputTimeoutTask = null;
            }
            
            // start the recognition Timer
            if (_recogTimout != 0) {
                startRecognitionTimer(_recogTimout);
            }
            
            //TODO: cancel speech recognition
            
                
            // if barge in enabled, send bareg in request (to transmitter)
			if ((_bargeIn) ) { //&&  (_activeRequestType == RequestType.playAndRecognize)){
				try {
					sendBargeinRequest();
				} catch (MrcpInvocationException e) {
					_logger.warn("MRCPv2 Status Code "+ e.getResponse().getStatusCode());
					_logger.warn(e, e);
				} catch (IOException e) {
					_logger.warn(e, e);
				} catch (InterruptedException e) {
					_logger.warn(e, e);
				}
			}
            
            
            //now we have the first char, we are waiting for a match
            _dtmfState = DtmfState.waitingForMatch;


            _charArray = new char[20];
            _charArray[0] = c;
            _length=1;    
            _inBuf =  new String(_charArray);
            _logger.debug("The first inBuf is : "+ _inBuf);


            // do the DTMF pattern matching
            checkForDtmfMatch(_inBuf);
            
        } else if (_dtmfState == DtmfState.waitingForMatch) {
            _logger.debug("   waiting for match...");

                //concatenate the new char to end of the dtmf string receievd up till now
          
                _charArray[_length++] = c;
                _inBuf =  new String(_charArray);
                _logger.debug("The new inBuf is: "+_inBuf);
                
                // do the DTMF pattern matching
                checkForDtmfMatch(_inBuf);

        } else {
            _logger.warn("Got dtmf signal while dtmf was not enabled by the client: "+c+  "  Discarding it.");
        }
    }


    private void checkForDtmfMatch(String c) {

   
        //do a regex match.  if it matches we are done
        Matcher m = _pattern.matcher(_inBuf);

        //if it matches
        if (m.find()) {
            _logger.debug("Got a dtmf match : "+_inBuf);
            _dtmfState = DtmfState.complete;
            
            //cancel the recog timer
            if (_noRecogTimeoutTask != null) {
                _noRecogTimeoutTask.cancel();
                _noRecogTimeoutTask = null;
            }
           
            //return the recognition results
           _dtmfListener.characterEventReceived(_inBuf,SpeechEventListener.EventType.recognitionMatch);
           

        }  else {
            _logger.debug("No match : "+_inBuf); 
        }
    }


    /* (non-Javadoc)
     * @see org.speechforge.cairo.client.SpeechClient#disableDtmf()
     */
    public void disableDtmf() {
        _dtmfState = DtmfState.notActive;
        _pattern = null;
        _dtmfListener = null;
        if (_noInputTimeoutTask != null) {
            _noInputTimeoutTask.cancel();
            _noInputTimeoutTask = null;
        }
        
        //cancel the recog timer
        if (_noRecogTimeoutTask != null) {
            _noRecogTimeoutTask.cancel();
            _noRecogTimeoutTask = null;
        }
        
    }


    /* (non-Javadoc)
     * @see org.speechforge.cairo.client.SpeechClient#enableDtmf(java.lang.String, org.speechforge.cairo.client.SpeechEventListener, long, long)
     */
    public void enableDtmf(String pattern, SpeechEventListener listener, long inputTimeout, long recogTimeout) {
        
        //check if there is already dtmf enabled (TODO if so throw exception)
        //if not go ahead and enable dtmf with this pattern
        if ((_dtmfState == DtmfState.notActive) || (_dtmfState == DtmfState.complete)) {
            _dtmfState = DtmfState.waitingForInput;
            
            //save the pattern
            _pattern =  Pattern.compile(pattern);
            
            //save the listener
            _dtmfListener = listener;
            
            //save the no recognition timeout value
            _recogTimout = recogTimeout;
            
            //TODO start the noInput Timer
            if (inputTimeout != 0) {
                startInputTimer(inputTimeout);
            }
            
            // Initialize the input buffer.  That is the string to be matched
            // against the pattern in the characterEventReceived method
            // when there is a match call the listener and this dtmf request is completed
            _inBuf = new String();

        
        } else {   //already an active dtmf recognition request
            _logger.warn("DTMF Recognition already active.");   
        }
        
    }
	
    

	public RecognitionResult playAndRecognizeBlocking(boolean urlPrompt, String prompt, String grammarUrl,
            boolean hotword) throws IOException, MrcpInvocationException, InterruptedException,
            IllegalValueException, NoMediaControlChannelException, InvalidSessionAddressException {
		playBlocking(urlPrompt, prompt);
		boolean attachGrammar = false;;
		int noInputTimeout =0;
		return recognizeBlocking(grammarUrl,hotword,attachGrammar,noInputTimeout);
    }

	public RecognitionResult playAndRecognizeBlocking(boolean urlPrompt, String prompt, Reader reader,
            boolean hotword) throws IOException, MrcpInvocationException, InterruptedException,
            IllegalValueException, NoMediaControlChannelException {
		_logger.warn("Not implemented.");
		return null;
    }

	public void playBlocking(boolean urlPrompt, String prompt) throws IOException, MrcpInvocationException,
            InterruptedException, NoMediaControlChannelException, InvalidSessionAddressException {
		
		AudioFormat synthFormat = rtpTransmitter.getFormat();
		String fileFormat = rtpTransmitter.getFileType();
		
		InputStream stream = synthesizer.synthesize(prompt, synthFormat, fileFormat, voiceName);
		
		//TODO: remove this step (converting stream to file) should just queue the stream
        String fname = Long.toString(System.currentTimeMillis())+".wav";
		if (fileFormat.equals("audio/x-au")) {
             fname = Long.toString(System.currentTimeMillis())+".au";
		} else if (fileFormat.equals("audio/x-wav")) {
             fname = Long.toString(System.currentTimeMillis())+".wav";
		} else {
			_logger.warn("Unrecognzied file format:"+ fileFormat+" Trying wav");
		}
		File f = streamToFile(stream,fname);
		
		rtpTransmitter.queueAudio(f, this);
		
    }

	private File streamToFile(InputStream inStream,String fname) throws IOException {

        File file = new File(tempDir+fname);
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
		
		BufferedInputStream in = new BufferedInputStream(inStream);

		byte[] buffer = new byte[256]; 
		while (true) { 
			int bytesRead = in.read(buffer);
			//_logger.trace("Read "+ bytesRead + "bytes.");
			if (bytesRead == -1) break; 
			out.write(buffer, 0, bytesRead); 
		} 
		_logger.info("Closing streams");
		in.close(); 
		out.close(); 

	    return file;
    }


	public SpeechRequest queuePrompt(boolean urlPormpt, String prompt) throws IOException,
            MrcpInvocationException, InterruptedException, NoMediaControlChannelException {
	    // TODO Auto-generated method stub
		_logger.warn("Not implemented.");
		return null;

    }

	public SpeechRequest recognize(String grammarUrl, boolean hotword, boolean attachGrammar,
            long noInputTimeout) throws IOException, MrcpInvocationException, InterruptedException,
            IllegalValueException, NoMediaControlChannelException {
		_logger.warn("Not implemented.");
		return null;
    }

	public SpeechRequest recognize(Reader reader, boolean hotword, boolean attachGrammar, long noInputTimeout)
            throws IOException, MrcpInvocationException, InterruptedException, IllegalValueException,
            NoMediaControlChannelException {
		_logger.warn("Not implemented.");
		return null;
    }

	public RecognitionResult recognizeBlocking(String grammarUrl, boolean hotword, boolean attachGrammar,
            long noInputTimeout) throws IOException, MrcpInvocationException, InterruptedException,
            IllegalValueException, NoMediaControlChannelException {

		


        URL grammar = null;
    	try {
    		grammar = new URL(grammarUrl);
		} catch (MalformedURLException e) {  
	         e.printStackTrace();  
		}		

		RtpS4EndPointingInputStream eStream = new RtpS4EndPointingInputStream();
		eStream.setMimeType(s4audio);
		eStream.setupStream(rtpReplicator);
		
        //start up the microphone
    	com.spokentech.speechdown.common.RecognitionResult rr = null;
        try {

			rr = recognizer.recognize(grammar, eStream, lmflg,  batchFlag, timeout);

        } catch (InstantiationException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }        
        
        
        //TODO:  Remove this hack.  Have a single RecognitionResult object used in both Cairo client and the cloud client
        //       Perhaps combine the two client libs.
    	RecognitionResult r = null;
        try {
	        r = RecognitionResult.constructResultFromString(rr.toString());
        } catch (InvalidRecogResultException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }

	    return r;
    }

	public RecognitionResult recognizeBlocking(Reader reader, boolean hotword, long noInputTimeout)
            throws IOException, MrcpInvocationException, InterruptedException, IllegalValueException,
            NoMediaControlChannelException {
    	_logger.warn("The recognize blocking(reader,hotwordFlag,timeout) method is not implemented");
        return null;
    }


	public MrcpRequestState sendBargeinRequest() throws IOException, MrcpInvocationException,
            InterruptedException {
	    // TODO Auto-generated method stub
	    return null;
    }

	public MrcpRequestState sendStartInputTimersRequest() throws MrcpInvocationException, IOException,
            InterruptedException {
	    // TODO Auto-generated method stub
	    return null;
    }

	public void setDefaultListener(SpeechEventListener listener) {
	     this.setListener(listener);
    }

	public void setListener(SpeechEventListener listener) {
        addListener(listener);
	    
    }

	public void shutdown() throws MrcpInvocationException, IOException, InterruptedException {
        // TODO Determine if there are active requests before stopping them
        
        // Stop any active requests
        try {
	        this.stopActiveRecognitionRequests();
        } catch (NoMediaControlChannelException e) {
	       _logger.debug("As part of shutting down the speech client, stopping active recognition requests.  No recog control channel so nothing to stop.");
        }
        
        //shutdown the timers
        //cancel the no input timer
        if (_noInputTimeoutTask != null) {
            _noInputTimeoutTask.cancel();
            _noInputTimeoutTask = null;
        }
        
        //cancel the recog timer
        if (_noRecogTimeoutTask != null) {
            _noRecogTimeoutTask.cancel();
            _noRecogTimeoutTask = null;
        }
	    
    }

	public void stopActiveRecognitionRequests() throws MrcpInvocationException, IOException,
            InterruptedException, NoMediaControlChannelException {
	    // TODO Auto-generated method stub
	    
    }

    /* (non-Javadoc)
     * @see org.speechforge.cairo.client.SpeechClient#turnOnBargeIn()
     */
    public void turnOnBargeIn() {
        _bargeIn = true;    
    }

    /* (non-Javadoc)
     * @see org.speechforge.cairo.client.SpeechClient#turnOffBargeIn()
     */
    public void turnOffBargeIn() {
       _bargeIn = false;
    }



	
	
	   /**
     * Tts event received.
     * 
     * @param event the event
     */
    private void ttsEventReceived(MrcpEvent event) {
        
        //first determine if this is event for a blocking request
        if ((_activeBlockingTts != null) && (event.getRequestID() == _activeBlockingTts.getRequestId() )) {
        
            if (MrcpEventName.SPEAK_COMPLETE.equals(event.getEventName())) {

                // if there is an active recognition request and bargein is enabled, start the timer
                if ((_bargeIn)&&(_activeRecognition != null)&&(!_activeRecognition.isCompleted())){
                    try {
                        sendStartInputTimersRequest();
                    } catch (MrcpInvocationException e) {
                        _logger.warn("MRCPv2 Status Code "+ e.getResponse().getStatusCode());
                        _logger.warn(e, e);
                    } catch (IOException e) {
                        _logger.warn(e, e);
                    } catch (InterruptedException e) {
                        _logger.warn(e, e);
                    }
                }
                
                //signal for the blocking call to check for unblocking
                synchronized (this) {
                    _activeBlockingTts.setCompleted(true);
                    //activeRequests.remove(key);
                    this.notifyAll();
                }
            }
            
            //else an event from an asynch request, just send the event on
        } else {
            
        	fireSynthEvent(event);

        }
        
   

    }

    /**
     * Recog event received.
     * 
     * @param event the event
     */
    private void recogEventReceived(MrcpEvent event) {
    	MrcpEventName eventName = event.getEventName();

    	//first determine if this is event for a blocking request
    	if ((_activeRecognition != null) && (event.getRequestID() == _activeRecognition.getRequestId() )) {


    		if (MrcpEventName.START_OF_INPUT.equals(eventName)) {
    			//TODO: DO you need to check if there is something to barge in on (what if the play already completed?  Or if there is no play in teh first place?
    			//used to check if part of a playAndRecognize.  But now that one can queue a play non-blocking and then call recognize with bargein enabled
    			//that check is no longer valid. 
    			if ((_bargeIn) ) { //&&  (_activeRequestType == RequestType.playAndRecognize)){
    				try {
    					sendBargeinRequest();
    				} catch (MrcpInvocationException e) {
    					_logger.warn("MRCPv2 Status Code "+ e.getResponse().getStatusCode());
    					_logger.warn(e, e);
    				} catch (IOException e) {
    					_logger.warn(e, e);
    				} catch (InterruptedException e) {
    					_logger.warn(e, e);
    				}
    			}

    		} else if (MrcpEventName.RECOGNITION_COMPLETE.equals(eventName)) {

    			//get the result and place in the active recog request object where it is retrieved by the blocking method
    			MrcpHeader completionCauseHeader = event.getHeader(MrcpHeaderName.COMPLETION_CAUSE);
    			CompletionCause completionCause = null;
    			try {
    				completionCause = (CompletionCause) completionCauseHeader.getValueObject();
    			} catch (IllegalValueException e) {
    				// TODO Auto-generated catch block
    				_logger.warn("Illegal Value getting the completion cause", e);

    			}

    			RecognitionResult r = null;
    			if (completionCause.getCauseCode() != 0) { 
    				r = null; 
    			} else {
    				try {
    					_logger.debug("Recognition event content: "+event.getContent());
    					r = RecognitionResult.constructResultFromString(event.getContent());
    					_logger.debug("recognition result text: "+r.getText());
    				} catch (InvalidRecogResultException e) {
    					_logger.warn("Illegal recognition result", e);
    					r = null;
    				}
    			}
    			_activeRecognition.setResult(r);


    			//signal for the blocking call to check for unblocking
    			synchronized (this) {
    				_activeRecognition.setCompleted(true);
    				this.notifyAll();
    			}
    		}
    	}
    	
		// else it is a non blocking requests, just forward on the event (with the recognition results)
		//    Always try to always send event (blocking or non) -- could be useful for status on client
    	RecognitionResult r = null;
    	if (MrcpEventName.RECOGNITION_COMPLETE.equals(eventName)) {
    		MrcpHeader completionCauseHeader = event.getHeader(MrcpHeaderName.COMPLETION_CAUSE);
    		CompletionCause completionCause = null;
    		try {
    			completionCause = (CompletionCause) completionCauseHeader.getValueObject();
    		} catch (IllegalValueException e) {
    			// TODO Auto-generated catch block
    			_logger.warn("Illegal Value getting the completion cause", e);
    		}
    		if (completionCause.getCauseCode() == 0) { 
    			try {
    				_logger.debug("Recognition event content: "+event.getContent());
    				r = RecognitionResult.constructResultFromString(event.getContent());
    				_logger.debug("recognition result text: "+r.getText());
    			} catch (InvalidRecogResultException e) {
    				_logger.warn("Illegal Recognition Result", e);
    				r = null;
    			}
    		}
    	}

    	fireRecogEvent(event,r);

    }

	public void addListener(SpeechEventListener listener) {
        synchronized (listenerList) {
        	listenerList.add(listener);
        }
	    
    }


	public void removeListener(SpeechEventListener listener) {
        synchronized (listenerList) {
        	listenerList.remove(listener);
        }
    }


    private void fireSynthEvent(final MrcpEvent event) {
        synchronized (listenerList) {
            Collection<SpeechEventListener> copy =  new java.util.ArrayList<SpeechEventListener>();        
            copy.addAll(listenerList);
            for (SpeechEventListener current : copy) {
                current.speechSynthEventReceived(event);
            }
        }
    }
    

    private void fireRecogEvent(final MrcpEvent event,RecognitionResult result) {
        synchronized (listenerList) {
            Collection<SpeechEventListener> copy =  new java.util.ArrayList<SpeechEventListener>();        
            copy.addAll(listenerList);
            for (SpeechEventListener current : copy) {
                current.recognitionEventReceived(event, result);
            }
        }
    }
    
    
    
    /**
     * The Class NoInputTimeoutTask.
     */
    private class NoInputTimeoutTask extends TimerTask {

        /* (non-Javadoc)
         * @see java.util.TimerTask#run()
         */
        @Override
        public void run() {
            synchronized (this) {
                _noInputTimeoutTask = null;
                if (_dtmfState == DtmfState.waitingForInput) {
                    _dtmfState = DtmfState.complete;
                    if (_dtmfListener != null) {
                        _dtmfListener.characterEventReceived(null,SpeechEventListener.EventType.noInputTimeout);
                    }
                }
            }
        }
        
    }
    
   //TODO: Combine the two timer tasks into a single task (as well as combining the accompanying startTimer methods)
    
    /**
     * Starts the input timers which trigger no-input-timeout if first dtmf key has not been depressed after the specified time.
     * 
     * @param noInputTimeout the amount of time to wait, in milliseconds, before triggering a no-input-timeout.
     * 
     * @return {@code true} if input timers were started or {@code false} if speech has already started.
     * 
     * @throws IllegalStateException if recognition is not in progress or if the input timers have already been started.
     */
    private synchronized boolean startInputTimer(long noInputTimeout) throws IllegalStateException {
        if (noInputTimeout <= 0) {
            throw new IllegalArgumentException("Illegal value for no-input-timeout: " + noInputTimeout);
        }
        if (_pattern == null) {
            throw new IllegalStateException("Recognition not in progress!");
        }
        if (_noInputTimeoutTask != null) {
            throw new IllegalStateException("InputTimer already started!");
        }

        boolean startInputTimers = (_dtmfState == DtmfState.waitingForInput); 
        if (startInputTimers) {
            _noInputTimeoutTask = new NoInputTimeoutTask();
            _timer.schedule(_noInputTimeoutTask, noInputTimeout);
        }

        return startInputTimers;
    } 
    
    
    /**
     * The Class NoRecogTimeoutTask.
     */
    private class NoRecogTimeoutTask extends TimerTask {

        /* (non-Javadoc)
         * @see java.util.TimerTask#run()
         */
        @Override
        public void run() {
            synchronized (this) {
                _noRecogTimeoutTask = null;
                if (_dtmfState == DtmfState.waitingForInput) {
                    _dtmfState = DtmfState.complete;
                    if (_dtmfListener != null) {
                        _dtmfListener.characterEventReceived(null,SpeechEventListener.EventType.noMatchTimeout);
                    }
                }
            }
        }
        
    }
    
    /**
     * Starts the input timers which trigger no-recognition-timeout if no recognition match has occurred after the specified time.
     * 
     * @param noMatchTimeout the amount of time to wait, in milliseconds, before triggering a no-recognition-timeout.
     * 
     * @return {@code true} if recog timers were started.
     * 
     * @throws IllegalStateException if recognition is not in progress or if the input timers have already been started.
     */
    private synchronized boolean startRecognitionTimer(long noMatchTimeout) throws IllegalStateException {
        if (noMatchTimeout <= 0) {
            throw new IllegalArgumentException("Illegal value for no-input-timeout: " + noMatchTimeout);
        }
        if (_pattern == null) {
            throw new IllegalStateException("Recognition not in progress!");
        }
        if (_noRecogTimeoutTask != null) {
            throw new IllegalStateException("InputTimer already started!");
        }

        boolean startRecognitionTimers = (_dtmfState == DtmfState.waitingForMatch); 
        if (startRecognitionTimers) {
            _noRecogTimeoutTask = new NoRecogTimeoutTask();
            _timer.schedule(_noRecogTimeoutTask, noMatchTimeout);
        }

        return startRecognitionTimers;
    }

    
    //methods for prompt play listener

	public void playCompleted() {
	    // TODO Auto-generated method stub
	    
    }


	public void playFailed(Exception arg0) {
	    // TODO Auto-generated method stub
	    
    }


	public void playInterrupted() {
	    // TODO Auto-generated method stub
	    
    }
}
