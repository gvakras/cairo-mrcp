/*
 * Zanzibar - Open source client for control of speech media resources.
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
package org.speechforge.cairo.client;

import java.io.IOException;
import java.io.Reader;

import javax.sip.SipException;

import org.mrcp4j.MrcpRequestState;
import org.mrcp4j.client.MrcpInvocationException;
import org.mrcp4j.message.header.IllegalValueException;
import org.speechforge.cairo.client.recog.RecognitionResult;
import org.speechforge.cairo.sip.SipSession;

// TODO: Auto-generated Javadoc
/**
 * SpeechClient API that does MRCP based speech recogntion.
 * 
 * @author Spencer Lord {@literal <}<a href="mailto:salord@users.sourceforge.net">salord@users.sourceforge.net</a>{@literal >}
 */
public interface SpeechClient {
    
    //TODO:  Re-Implement the non blocking calls.  Commented them out for the time being to get something working.
    //TODO:  Redesign the SpeechRequest object. reduce to essential components.  remove MRCP references...
    
       
    /**
     * Play blocking.
     * 
     * @param urlPrompt the url prompt
     * @param prompt the prompt
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws MrcpInvocationException the mrcp invocation exception
     * @throws InterruptedException the interrupted exception
     */
    public void playBlocking(boolean urlPrompt, String prompt)  throws IOException, MrcpInvocationException, InterruptedException;

    
    /**
     * Recognize blocking.
     * 
     * @param grammarUrl the grammar url
     * 
     * @return the recognition result
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws MrcpInvocationException the mrcp invocation exception
     * @throws InterruptedException the interrupted exception
     * @throws IllegalValueException the illegal value exception
     */
    public RecognitionResult recognizeBlocking(String grammarUrl, boolean hotword, boolean attachGrammar)throws IOException, MrcpInvocationException, InterruptedException, IllegalValueException;


    /**
     * Recognize blocking.
     * 
     * @param reader the reader
     * 
     * @return the recognition result
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws MrcpInvocationException the mrcp invocation exception
     * @throws InterruptedException the interrupted exception
     * @throws IllegalValueException the illegal value exception
     */
    public RecognitionResult recognizeBlocking(Reader reader, boolean hotword)throws IOException, MrcpInvocationException, InterruptedException, IllegalValueException;

    
    /**
     * Play and recognize blocking.  This version of play and recognize receives a url to the grammar.
     * 
     * @param urlPrompt the url prompt. if true the prompt parameter is a url to a file containing the prompt else its the prompt itself.
     * @param prompt the prompt
     * @param grammarUrl the grammar url
     * 
     * @return the recognition result
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws MrcpInvocationException the mrcp invocation exception
     * @throws InterruptedException the interrupted exception
     * @throws IllegalValueException the illegal value exception
     */
    public RecognitionResult playAndRecognizeBlocking(boolean urlPrompt, String prompt, String grammarUrl, boolean hotword) throws IOException, MrcpInvocationException, InterruptedException, IllegalValueException;
    
    /**
     * Play and recognize blocking.  This version of play and recognize receievs a Reader to the grammar
     * 
     * @param prompt the prompt
     * @param reader the reader
     * 
     * @return the recognition result
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws MrcpInvocationException the mrcp invocation exception
     * @throws InterruptedException the interrupted exception
     * @throws IllegalValueException the illegal value exception
     */
    public RecognitionResult playAndRecognizeBlocking(boolean urlPrompt, String prompt, Reader reader, boolean hotword) throws IOException, MrcpInvocationException, InterruptedException, IllegalValueException;
        
    
    /**
     * Tun on barge in.
     */
    public void tunOnBargeIn();
    
    /**
     * Turn off barge in.
     */
    public void turnOffBargeIn();



    /**
     * Send start input timers request.
     * 
     * @return the mrcp request state
     * 
     * @throws MrcpInvocationException the mrcp invocation exception
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws InterruptedException the interrupted exception
     */
    //TODO: Move the MrcpRequestState out of the interface (hide MRCP)
    public MrcpRequestState sendStartInputTimersRequest() throws MrcpInvocationException, IOException, InterruptedException;

    /**
     * Sets the default listener.  To set the listener for methods that don't have a listener parameter.
     * 
     * @param listener the new default listener
     */
    public void setDefaultListener(SpeechEventListener listener);
    
 
    /**
     * Queue prompt.  This is a non-blocking call.
     * 
     * @param urlPormpt the url pormpt
     * @param prompt the prompt
     * @param listener the listener
     * 
     * @return the speech request
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws MrcpInvocationException the mrcp invocation exception
     * @throws InterruptedException the interrupted exception
     */
    public SpeechRequest queuePrompt(boolean urlPormpt, String prompt, SpeechEventListener listener)  throws IOException, MrcpInvocationException, InterruptedException;    

   
    /**
     * Enable dtmf.  If dtmf is already enabled, 
     * (replace old pattern and listener or throw exception?)
     * 
     * @param pattern the pattern
     * @param listener the listener
     * @param inputTimeout the input timeout
     * @param recogTimeout the recog timeout
     * 
     */
    public void enableDtmf(String pattern, SpeechEventListener listener, long inputTimeout, long recogTimeout) ;
   
    /**
     * Disable dtmf.
     */
    public void disableDtmf();
    
    public SpeechRequest recognize(String grammarUrl, boolean hotword, boolean attachGrammar, SpeechEventListener listener) throws IOException, MrcpInvocationException, InterruptedException, IllegalValueException ;


    //Methods not implemented yet....
    
    /**
     * Init.
     * 
     * @param urlPrompt the url prompt
     * @param prompt the prompt
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws MrcpInvocationException the mrcp invocation exception
     * @throws InterruptedException the interrupted exception
     */
    //public void initLocalSipAgent(int localSipPort, String mySipAddress);
    
    /**
     * Start session.
     * 
     * @param cairoSipPort the cairo sip port
     * @param cairoSipHostName the cairo sip host name
     * @param cairoSipAddress the cairo sip address
     * @param localRtpPort the local rtp port
     * @param sessionName the session name
     */
    //public  void startSession(int cairoSipPort, String cairoSipHostName, String cairoSipAddress, int localRtpPort, String sessionName);
 
    
    /**
     * Recognize.  Non-blocking method.
     * 
     * @param grammarUrl the grammar url
     * 
     * @return the recognition result
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws MrcpInvocationException the mrcp invocation exception
     * @throws InterruptedException the interrupted exception
     * @throws IllegalValueException the illegal value exception
     */
    //TODO: add a timeout value to the recognize commands (or perhasp to the sendStartTimer method
    //public SpeechRequest recognize(String grammarUrl, SpeechEventListener listener)throws IOException, MrcpInvocationException, InterruptedException, IllegalValueException;  
    
    //public  SpeechRequest playAndRecognize(String prompt, String grammarUrl, SpeechEventListener listener) throws IOException, MrcpInvocationException, InterruptedException, IllegalValueException;

}
