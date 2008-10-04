package org.speechforge.cairo.client;



import org.speechforge.cairo.client.SpeechClient;
import org.speechforge.cairo.client.SpeechEventListener;
import org.speechforge.cairo.client.SpeechRequest.RequestType;
import org.speechforge.cairo.client.recog.InvalidRecognitionResultException;
import org.speechforge.cairo.client.recog.RecognitionResult;
import org.speechforge.cairo.rtp.NativeMediaClient;
import org.speechforge.cairo.sip.SipSession;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sip.SipException;

import org.apache.log4j.Logger;
import org.mrcp4j.MrcpEventName;
import org.mrcp4j.MrcpMethodName;
import org.mrcp4j.MrcpRequestState;
import org.mrcp4j.client.MrcpChannel;
import org.mrcp4j.client.MrcpEventListener;
import org.mrcp4j.client.MrcpFactory;
import org.mrcp4j.client.MrcpInvocationException;
import org.mrcp4j.client.MrcpProvider;
import org.mrcp4j.message.MrcpEvent;
import org.mrcp4j.message.MrcpResponse;
import org.mrcp4j.message.header.CompletionCause;
import org.mrcp4j.message.header.IllegalValueException;
import org.mrcp4j.message.header.MrcpHeader;
import org.mrcp4j.message.header.MrcpHeaderName;
import org.mrcp4j.message.request.MrcpRequest;

// TODO: Auto-generated Javadoc
/**
 * SpeechClient Implementation.
 * 
 * @author Spencer Lord {@literal <}<a href="mailto:salord@users.sourceforge.net">salord@users.sourceforge.net</a>{@literal >}
 */
public class SpeechClientImpl implements MrcpEventListener, SpeechClient, SpeechClientProvider, SpeechEventListener {

    /** The _logger. */
    private static Logger _logger = Logger.getLogger(SpeechClientImpl.class);
 
    //InetAddress _cairoSipInetAddress = null;
    //private  String _cairoSipHostName;
    //private  int _peerHostPort;
    
    /** The _tts channel. */
    private MrcpChannel _ttsChannel;
    
    /** The _recog channel. */
    private MrcpChannel _recogChannel;
    
    /** The _barge in flag */
    private boolean _bargeIn = false;

    //used to construct mrcp channels (in the static metods below)
    private static String protocol = MrcpProvider.PROTOCOL_TCP_MRCPv2;
    private static MrcpFactory factory = MrcpFactory.newInstance();
    private static MrcpProvider provider = factory.createProvider();
    
    
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

    //TODO: Deal with dead calls -- clean up map periocially or leases or ...
    /** private Map<String,SpeechRequest> activeRequests;
     *  map of active reuests for non-blocking calls.  This is turning out to be more difficult than expected
     *  so only supporting blocking calls for now and using the following state variables rather than the map */
    RequestType _activeRequestType;
    SpeechRequest _activeBlockingTts;
    
    /** The _active recognition. */
    SpeechRequest _activeRecognition;

    /** The default listener. */
    private SpeechEventListener defaultListener;
    
    /**
     * Instantiates a new speech client impl.
     * 
     * @param session the session
     */
    public SpeechClientImpl(MrcpChannel tts, MrcpChannel recog) {
        super();

        _ttsChannel = tts;
        _ttsChannel.addEventListener(this);
        _recogChannel = recog;
        _recogChannel.addEventListener(this); 
        //activeRequests = new HashMap<String,SpeechRequest>();
    }


    /* (non-Javadoc)
     * @see org.mrcp4j.client.MrcpEventListener#eventReceived(org.mrcp4j.message.MrcpEvent)
     */
    public void eventReceived(MrcpEvent event) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("MRCP event received:\n" + event.toString());
        }

        try {
            switch (event.getChannelIdentifier().getResourceType()) {
            case SPEECHSYNTH:
                ttsEventReceived(event);
                break;

            case SPEECHRECOG:
                recogEventReceived(event);
                break;

            default:
                _logger.warn("Unexpected value for event resource type!");
                break;
            }
        } catch (IllegalValueException e) {
            _logger.warn("Illegal value for event resource type!", e);
        }
   }

    /**
     * Tts event received.
     * 
     * @param event the event
     */
    private void ttsEventReceived(MrcpEvent event) {
        if (MrcpEventName.SPEAK_COMPLETE.equals(event.getEventName())) {
            //TODO: DO you need to check if there is something to start a timer upon?  what if the speech is already complete?  I think this 
            // us causing periodic 402 codes (method not valid on this state).
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
            _activeBlockingTts.getListener().ttsCompletedEventReceived(event);
        }
    }

    /**
     * Recog event received.
     * 
     * @param event the event
     */
    private void recogEventReceived(MrcpEvent event) {
        MrcpEventName eventName = event.getEventName();

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
            MrcpHeader completionCauseHeader = event.getHeader(MrcpHeaderName.COMPLETION_CAUSE);
            CompletionCause completionCause = null;
            try {
                completionCause = (CompletionCause) completionCauseHeader.getValueObject();
            } catch (IllegalValueException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            RecognitionResult r = null;
            if (completionCause.getCauseCode() != 0) { 
                r = null; 
            } else {
                try {
                    _logger.info("Recognition event content: "+event.getContent());
                    r = RecognitionResult.constructResultFromString(event.getContent());
                    _logger.info("recognition result text: "+r.getText());
                } catch (InvalidRecognitionResultException e) {
                    e.printStackTrace();
                    r = null;
                }
            }
            _activeRecognition.setResult(r);
            _activeRecognition.getListener().recognitionEventReceived(event, r);
        }
    }

    /* (non-Javadoc)
     * @see org.speechforge.zanzibar.SpeechClient#sendStartInputTimersRequest()
     */
    public MrcpRequestState sendStartInputTimersRequest()
      throws MrcpInvocationException, IOException, InterruptedException {

        // construct request
        MrcpRequest request = _recogChannel.createRequest(MrcpMethodName.START_INPUT_TIMERS);

        // send request
        MrcpResponse response = _recogChannel.sendRequest(request);
        if (_logger.isDebugEnabled()) {
            _logger.debug("MRCP response received:\n" + response.toString());
        }
        return response.getRequestState();
    }

    /**
     * Send bargein request.
     * 
     * @return the mrcp request state
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws MrcpInvocationException the mrcp invocation exception
     * @throws InterruptedException the interrupted exception
     */
    private MrcpRequestState sendBargeinRequest()
      throws IOException, MrcpInvocationException, InterruptedException {

        // construct request
        MrcpRequest request = _ttsChannel.createRequest(MrcpMethodName.BARGE_IN_OCCURRED);

        // send request
        MrcpResponse response = _ttsChannel.sendRequest(request);

        if (_logger.isDebugEnabled()) {
            _logger.debug("MRCP response received:\n" + response.toString());
        }

        return response.getRequestState();
    }


    /**
     * TODOC.
     * 
     * @param prompt the prompt
     * @param urlPrompt the url prompt
     * @param listener the listener
     * 
     * @return recognition result string
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws MrcpInvocationException the mrcp invocation exception
     * @throws InterruptedException the interrupted exception
     */
    public SpeechRequest play(boolean urlPrompt, String prompt, SpeechEventListener listener) throws IOException, MrcpInvocationException, InterruptedException {

        // speak request
        MrcpRequest request = _ttsChannel.createRequest(MrcpMethodName.SPEAK);
        if (!urlPrompt) {
           request.setContent("text/plain", null, prompt);
        } else {
            request.setContent("text/uri-list", null, prompt); 
        }
        MrcpResponse response = _ttsChannel.sendRequest(request);
           


        if (_logger.isDebugEnabled()) {
            _logger.debug("MRCP response received:\n" + response.toString());
        }
        
        //_activeRequestType = RequestType.play;
        SpeechRequest queuedTts = new SpeechRequest(response.getRequestID(),RequestType.play,false,listener);   
        queuedTts.setBlockingCall(false);

        //if no listener is passed in, use the default one (used by some APIs that set the listener once at client constrution time)
        if (listener == null) {
            _activeBlockingTts.setListener(defaultListener);
        }

        return queuedTts;

    }

    
    /**
     * Recognize.
     * 
     * @param grammarUrl the grammar url
     * @param listener the listener
     * 
     * @return the speech request
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws MrcpInvocationException the mrcp invocation exception
     * @throws InterruptedException the interrupted exception
     * @throws IllegalValueException the illegal value exception
     */
    public SpeechRequest recognize(String grammarUrl, boolean hotword, boolean attachGrammar, SpeechEventListener listener) throws IOException, MrcpInvocationException, InterruptedException, IllegalValueException {


        MrcpRequest request = constructRecogRequest(grammarUrl,hotword, attachGrammar);
        if (hotword) {
           request.addHeader(MrcpHeaderName.RECOGNITION_MODE.constructHeader("hotword"));
        }
        
        
        _logger.info("REQUEST: "+request.toString());
        MrcpResponse response = _recogChannel.sendRequest(request);

        if (_logger.isDebugEnabled()) {
            _logger.debug("MRCP response received:\n" + response.toString());
        }

        if (response.getRequestState().equals(MrcpRequestState.COMPLETE)) {
            throw new RuntimeException("Recognition failed to start!");
            
        }
        //_activeRequestType = RequestType.recognize;
        _activeRecognition = new SpeechRequest(response.getRequestID(),RequestType.recognize,false,this);   
        _activeRecognition.setBlockingCall(false);
        
        //if no listener is passed in, use the default one (used by some APIs that set the listener once at client constrution time)
        if (listener == null) {
            _activeRecognition.setListener(defaultListener);
        } else {
            _activeRecognition.setListener(listener);
        }
        
        //activeRequests.put(String.valueOf(response.getRequestID()),call);   

//      MrcpHeader completionCauseHeader = _mrcpEvent.getHeader(MrcpHeaderName.COMPLETION_CAUSE);
//      CompletionCause completionCause = (CompletionCause) completionCauseHeader.getValueObject();          
//      return (completionCause.getCauseCode() == 0) ? _mrcpEvent.getContent() : null ;
        
        return _activeRecognition;
    }

    
    /**
     * Play and recognize.
     * 
     * @param prompt the prompt
     * @param grammarUrl the grammar url
     * @param listener the listener
     * 
     * @return the speech request
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws MrcpInvocationException the mrcp invocation exception
     * @throws InterruptedException the interrupted exception
     * @throws IllegalValueException the illegal value exception
     */
    public  SpeechRequest playAndRecognize(boolean urlPrompt, String prompt, String grammarUrl, SpeechEventListener listener, boolean hotword)
    throws IOException, MrcpInvocationException, InterruptedException, IllegalValueException {

        MrcpRequest request = constructRecogRequest(grammarUrl, hotword, true);
        SpeechRequest speechRequest = internalPlayAndRecogize(urlPrompt, prompt, listener, request);

        return speechRequest;  

    }


    /**
     * Internal play and recogize.
     * 
     * @param urlPrompt the url prompt
     * @param prompt the prompt
     * @param listener the listener
     * @param request the request
     * 
     * @return the speech request
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws MrcpInvocationException the mrcp invocation exception
     * @throws InterruptedException the interrupted exception
     */
    private SpeechRequest internalPlayAndRecogize(boolean urlPrompt, String prompt, SpeechEventListener listener, MrcpRequest request) throws IOException, MrcpInvocationException, InterruptedException {
          MrcpResponse response = _recogChannel.sendRequest(request);

          if (_logger.isDebugEnabled()) {
              _logger.debug("MRCP response received:\n" + response.toString());
          }

          if (response.getRequestState().equals(MrcpRequestState.COMPLETE)) {
              throw new RuntimeException("Recognition failed to start!");
          }
          
          //_activeRequestType = RequestType.playAndRecognize;

          _activeRecognition = new SpeechRequest(response.getRequestID(),RequestType.playAndRecognize,false,listener);   
          _activeRecognition.setBlockingCall(false);
          
          //if no listener is passed in, use the default one (used by some APIs that set the listener once at client constrution time)
          if (listener == null) {
              _activeRecognition.setListener(defaultListener);
          }
          
          //activeRequests.put(String.valueOf(response.getRequestID()),recognizeCall);   

          // speak request
          request = _ttsChannel.createRequest(MrcpMethodName.SPEAK);
          if (!urlPrompt) {
              request.setContent("text/plain", null, prompt);
           } else {
               request.setContent("text/uri-list", null, prompt); 
           }
          response = _ttsChannel.sendRequest(request);

          if (_logger.isDebugEnabled()) {
              _logger.debug("MRCP response received:\n" + response.toString());
          }
          
          _activeBlockingTts = new SpeechRequest(response.getRequestID(),RequestType.playAndRecognize,false,this);   
          _activeBlockingTts.setBlockingCall(false);
          //activeRequests.put(String.valueOf(response.getRequestID()),playCall);  
          
          //link the play and recogize requests.
          _activeBlockingTts.setLinkedRequest(_activeRecognition);
          _activeRecognition.setLinkedRequest(_activeBlockingTts);
          
          return _activeRecognition;

//            MrcpHeader completionCauseHeader = _mrcpEvent.getHeader(MrcpHeaderName.COMPLETION_CAUSE);
//            CompletionCause completionCause = (CompletionCause) completionCauseHeader.getValueObject();          
//            return (completionCause.getCauseCode() == 0) ? _mrcpEvent.getContent() : null ;
    }


    /**
     * Construct recog request.
     * 
     * @param grammarUrl the grammar url
     * 
     * @return the mrcp request
     * 
     * @throws MalformedURLException the malformed URL exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private MrcpRequest constructRecogRequest(String grammarUrl, boolean hotword, boolean attachGrammar) throws MalformedURLException, IOException {

          // recog request
          MrcpRequest request = _recogChannel.createRequest(MrcpMethodName.RECOGNIZE);
          request.addHeader(MrcpHeaderName.START_INPUT_TIMERS.constructHeader(Boolean.FALSE));
          if (hotword) {
              request.addHeader(MrcpHeaderName.RECOGNITION_MODE.constructHeader("hotword"));
           }

          if (attachGrammar) {
              URL gUrl = new URL(grammarUrl);
              request.setContent("application/jsgf", null, gUrl);
          } else {
              request.setContent("text/uri-list", null, grammarUrl);
          }
        return request;
    } 

    
    /**
     * Construct recog request.
     * 
     * @param reader the reader
     * 
     * @return the mrcp request
     * 
     * @throws MalformedURLException the malformed URL exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private MrcpRequest constructRecogRequest(Reader reader, boolean hotword) throws MalformedURLException, IOException {

        BufferedReader in  = new BufferedReader(reader);
        StringBuilder sb = new StringBuilder();

        String line = null;
        while ((line = in.readLine()) != null) {
            sb.append(line);
        }

        // recog request
        MrcpRequest request = _recogChannel.createRequest(MrcpMethodName.RECOGNIZE);
        request.addHeader(MrcpHeaderName.START_INPUT_TIMERS.constructHeader(Boolean.FALSE));
        if (hotword) {
            request.addHeader(MrcpHeaderName.RECOGNITION_MODE.constructHeader("hotword"));
         }
        request.setContent("application/jsgf", null, sb.toString());
        return request;
    } 

    
    /**



    /* (non-Javadoc)
     * @see org.speechforge.zanzibar.SpeechClient#playBlocking(java.lang.Boolean, java.lang.String)
     */
    public void playBlocking(boolean urlPrompt, String prompt) throws IOException, MrcpInvocationException, InterruptedException {
        _activeBlockingTts = this.play(urlPrompt, prompt,this);
        //Block...
        //ActiveRequest request = activeRequests.get(String.valueOf(response.getRequestID()));
        _activeBlockingTts.setBlockingCall(true);
        while (!_activeBlockingTts.isCompleted()) {
            synchronized (this) {        
                try {
                    this.wait(1000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        return;
    }


    /* (non-Javadoc)
     * @see org.speechforge.zanzibar.SpeechClient#recognizeBlocking(java.lang.String)
     */
    public RecognitionResult recognizeBlocking(String grammarUrl, boolean hotword, boolean attachGrammar) throws IOException, MrcpInvocationException, InterruptedException, IllegalValueException {
        _activeRecognition = this.recognize(grammarUrl, hotword, attachGrammar, this);
        //Block...
        //ActiveRequest request = activeRequests.get(String.valueOf(response.getRequestID()));
        
        _activeRecognition.setBlockingCall(true);
        while (!_activeRecognition.isCompleted()) {
            synchronized (this) {        
                try {
                    this.wait(1000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
  
        }
        return _activeRecognition.getResult();
    }
    

    /* (non-Javadoc)
     * @see org.speechforge.zanzibar.SpeechClient#playAndRecognizeBlocking(java.lang.Boolean, java.lang.String, java.lang.String)
     */
    public RecognitionResult playAndRecognizeBlocking(boolean urlPrompt, String prompt, String grammarUrl, boolean hotword) throws IOException, MrcpInvocationException, InterruptedException, IllegalValueException {
        MrcpRequest mrcpRequest = constructRecogRequest(grammarUrl, hotword, true);
        _activeRecognition = internalPlayAndRecogize(urlPrompt, prompt, this, mrcpRequest);
        
        //Block...
        //ActiveRequest request = activeRequests.get(String.valueOf(response.getRequestID()));
        
        _activeRecognition.setBlockingCall(true);
        while (!_activeRecognition.isCompleted()) {
            synchronized (this) {        
                try {
                    this.wait(1000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
  
        }
        return _activeRecognition.getResult();
    }
    
    /* (non-Javadoc)
     * @see org.speechforge.zanzibar.SpeechEventListener#recognitionEventReceived(org.mrcp4j.message.MrcpEvent, org.speechforge.cairo.server.recog.RecognitionResult)
     */
    public void recognitionEventReceived(MrcpEvent event, RecognitionResult r) {
        //String key = String.valueOf(event.getRequestID());
        //if (this.activeRequests.containsKey(key)) {
           //SpeechRequest request = activeRequests.get(key);
            synchronized (this) {
                _activeRecognition.setCompleted(true);
                //activeRequests.remove(key);
                this.notifyAll();
            }
        //} 
    }


    /* (non-Javadoc)
     * @see org.speechforge.zanzibar.SpeechEventListener#ttsCompletedEventReceived(org.mrcp4j.message.MrcpEvent)
     */
    public void ttsCompletedEventReceived(MrcpEvent event) {

        //String key = String.valueOf(event.getRequestID());
        //if (this.activeRequests.containsKey(key)) {
           //SpeechRequest request = activeRequests.get(key);
            synchronized (this) {
                _activeBlockingTts.setCompleted(true);
                //activeRequests.remove(key);
                this.notifyAll();
            }
        //}    
    }
    
    /* (non-Javadoc)
     * @see org.speechforge.zanzibar.SpeechClient#tunOnBargeIn()
     */
    public void tunOnBargeIn() {
        _bargeIn = true;    
    }

    /* (non-Javadoc)
     * @see org.speechforge.zanzibar.SpeechClient#turnOffBargeIn()
     */
    public void turnOffBargeIn() {
       _bargeIn = false;
    }




    /* (non-Javadoc)
     * @see org.speechforge.zanzibar.SpeechClient#setDefaultListener(org.speechforge.zanzibar.SpeechEventListener)
     */
    public void setDefaultListener(SpeechEventListener listener) {
        this.defaultListener = listener;
    }


    /* (non-Javadoc)
     * @see org.speechforge.zanzibar.SpeechClient#playAndRecognizeBlocking(java.lang.String, java.io.Reader)
     */
    public RecognitionResult playAndRecognizeBlocking(boolean urlPrompt, String prompt, Reader reader, boolean hotword) throws IOException, MrcpInvocationException, InterruptedException, IllegalValueException {
        MrcpRequest mrcpRequest = constructRecogRequest(reader,hotword);
        SpeechRequest request = internalPlayAndRecogize(urlPrompt,prompt, this, mrcpRequest);
        
        //Block...
        //ActiveRequest request = activeRequests.get(String.valueOf(response.getRequestID()));
        
        request.setBlockingCall(true);
        while (!_activeRecognition.isCompleted()) {
            synchronized (this) {        
                try {
                    this.wait(1000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
  
        }
        return request.getResult();
    }


    /* (non-Javadoc)
     * @see org.speechforge.zanzibar.SpeechClient#recognizeBlocking(java.io.Reader)
     */
    public RecognitionResult recognizeBlocking(Reader reader, boolean hotword) throws IOException, MrcpInvocationException, InterruptedException, IllegalValueException {
        _logger.warn("The recognize blocking(reader,hotwordFlag) method is not implemented");
        return null;
    }


    /* (non-Javadoc)
     * @see org.speechforge.zanzibar.SpeechClient#queuePrompt(java.lang.Boolean, java.lang.String, org.speechforge.zanzibar.SpeechEventListener)
     */
    public SpeechRequest queuePrompt(boolean urlPrompt, String prompt, SpeechEventListener listener) throws IOException, MrcpInvocationException, InterruptedException {
        return play(urlPrompt, prompt,listener);
    }



    /* (non-Javadoc)
     * @see org.speechforge.zanzibar.SpeechClientProvider#characterEventReceived(java.lang.String)
     */
    public void characterEventReceived(char c) {
        _logger.info("speechclient.chareventreceived: "+c);
        
        if (_dtmfState == DtmfState.waitingForInput) {
            _logger.info("   waitingfor input...");
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
                
            
            //now we have the first char, we are waiting for a match
            _dtmfState = DtmfState.waitingForMatch;


            _charArray = new char[20];
            _charArray[0] = c;
            _length=1;    
            _inBuf =  new String(_charArray);
            _logger.info("The first inBuf is : "+ _inBuf);


            // do the DTMF pattern matching
            checkForDtmfMatch(_inBuf);
            
        } else if (_dtmfState == DtmfState.waitingForMatch) {
            _logger.info("   waiting for match...");
            try {
                //concatenate the new char to end of the dtmf string receievd up till now

              
                _charArray[_length++] = c;
                _inBuf =  new String(_charArray);
                _logger.info("The new inBuf is: "+_inBuf);
                
                // do the DTMF pattern matching
                checkForDtmfMatch(_inBuf);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            _logger.warn("Got dtmf signal while dtmf was not enabled by the client: "+c+  "  Discarding it.");
        }
    }


    private void checkForDtmfMatch(String c) {

   
        //do a regex match.  if it matches we are done
        Matcher m = _pattern.matcher(_inBuf);

        //if it matches
        if (m.find()) {
            _logger.info("Got a dtmf match : "+_inBuf);
            
            //cancel the recog timer
            if (_noRecogTimeoutTask != null) {
                _noRecogTimeoutTask.cancel();
                _noRecogTimeoutTask = null;
            }
           
            //return the recognition results
           _dtmfListener.characterEventReceived(_inBuf,SpeechEventListener.EventType.recognitionMatch);
           
           // set state to not active (and ready for next request)
           _pattern = null;
           _dtmfListener = null;
           _dtmfState = DtmfState.notActive;
        }  else {
            _logger.info("No match : "+_inBuf); 
        }
    }


    /* (non-Javadoc)
     * @see org.speechforge.zanzibar.SpeechClient#disableDtmf()
     */
    public void disableDtmf() {
        _dtmfState = DtmfState.notActive;
        _pattern = null;
        _dtmfListener = null;
        if (_noInputTimeoutTask != null) {
            _noInputTimeoutTask.cancel();
            _noInputTimeoutTask = null;
        }
        
    }


    /* (non-Javadoc)
     * @see org.speechforge.zanzibar.SpeechClient#enableDtmf(java.lang.String, org.speechforge.zanzibar.SpeechEventListener, long, long)
     */
    public void enableDtmf(String pattern, SpeechEventListener listener, long inputTimeout, long recogTimeout) {
        
        //check if there is already dtmf enabled (TODO if so throw exception)
        //if not go ahead and enable dtmf with this pattern
        if (_dtmfState == DtmfState.notActive) {
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


    /* (non-Javadoc)
     * @see org.speechforge.zanzibar.SpeechEventListener#characterEventReceived(java.lang.String, org.speechforge.zanzibar.SpeechEventListener.EventType)
     */
    public void characterEventReceived(String c, EventType status) {
        // TODO Auto-generated method stub
        
    }


    public void bye() {
        // TODO Auto-generated method stub
        
    }

    
    //Utility methods
    
    public static MrcpChannel createTtsChannel(String xmitterChannelId, InetAddress remoteHostAdress, int xmitterPort) throws IllegalArgumentException, IllegalValueException, IOException {
        //Construct the MRCP Channels    
        MrcpChannel ttsChannel = provider.createChannel(xmitterChannelId, remoteHostAdress, xmitterPort, protocol);
        return ttsChannel;
    }
    
    public static MrcpChannel createRecogChannel(String receiverChannelId, InetAddress remoteHostAdress, int receiverPort) throws IllegalArgumentException, IllegalValueException, IOException {
        //Construct the MRCP Channels
        MrcpChannel recogChannel = provider.createChannel(receiverChannelId, remoteHostAdress, receiverPort, protocol);
        return recogChannel;
    }
    
    public static NativeMediaClient createMediaClient(int localRtpPort, InetAddress rserverHost, int remoteRtpPort) throws IllegalArgumentException, IllegalValueException, IOException {
       NativeMediaClient mediaClient = new NativeMediaClient(localRtpPort, rserverHost, remoteRtpPort);
       mediaClient.startTransmit();
       return mediaClient;
    }
}
