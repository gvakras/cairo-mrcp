/**
 * TODOC
 * 
 */
package com.onomatopia.cairo.server.tts;

import com.onomatopia.cairo.server.MrcpGenericChannel;

import java.io.File;
import java.io.IOException;

import javax.media.rtp.InvalidSessionAddressException;
import javax.sound.sampled.AudioFileFormat;

import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.VoiceManager;
import com.sun.speech.freetts.audio.AudioPlayer;
import com.sun.speech.freetts.audio.SingleFileAudioPlayer;

import org.apache.commons.lang.Validate;
import org.mrcp4j.MrcpRequestState;
import org.mrcp4j.message.MrcpResponse;
import org.mrcp4j.message.header.MrcpHeader;
import org.mrcp4j.message.header.MrcpHeaderFactory;
import org.mrcp4j.message.request.StopRequest;
import org.mrcp4j.message.request.MrcpRequestFactory.UnimplementedRequest;
import org.mrcp4j.server.MrcpSession;
import org.mrcp4j.server.provider.SpeechSynthRequestHandler;

/**
 * TODOC
 * @author Niels
 *
 */
public class MrcpSpeechSynthChannel extends MrcpGenericChannel implements SpeechSynthRequestHandler {

//    private static short IDLE = 0;
//    private static short SPEAKING = 1;
//    private static short PAUSED = 2;
//
//    volatile short _state = IDLE;

    //private String _channelID;
    private PromptGenerator _promptGenerator;
    private RTPSpeechSynthChannel _rtpChannel;

    /**
     * TODOC
     * @param channelID 
     * @param basePromptDir 
     * @param rtpChannel 
     */
    public MrcpSpeechSynthChannel(String channelID, RTPSpeechSynthChannel rtpChannel, File basePromptDir) {
        //_channelID = channelID;
        _rtpChannel = rtpChannel;
        _promptGenerator = new PromptGenerator(channelID, basePromptDir);
    }

    /* (non-Javadoc)
     * @see org.mrcp4j.server.provider.SpeechSynthRequestHandler#speak(org.mrcp4j.message.request.MrcpRequestFactory.UnimplementedRequest, org.mrcp4j.server.MrcpSession)
     */
    public synchronized MrcpResponse speak(UnimplementedRequest request, MrcpSession session) {
        MrcpRequestState requestState = MrcpRequestState.COMPLETE;
        short statusCode = -1;

        if (request.hasContent()) {
            String contentType = request.getContentType();
            if (contentType.equalsIgnoreCase("text/plain")) {
                String text = /*"What's " +*/ request.getContent();
                try {
                    File promptFile = _promptGenerator.generatePrompt(text);
                    int state = _rtpChannel.queuePrompt(promptFile);
                    requestState = (state == RTPSpeechSynthChannel.IDLE) ? MrcpRequestState.IN_PROGRESS : MrcpRequestState.PENDING;
                    statusCode = MrcpResponse.STATUS_SUCCESS;
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    statusCode = MrcpResponse.STATUS_SERVER_INTERNAL_ERROR;
                } catch (InvalidSessionAddressException e) {
                    e.printStackTrace();
                    statusCode = MrcpResponse.STATUS_OPERATION_FAILED;
                } catch (IOException e) {
                    e.printStackTrace();
                    statusCode = MrcpResponse.STATUS_OPERATION_FAILED;
                }
            } else {
                statusCode = MrcpResponse.STATUS_UNSUPPORTED_HEADER_VALUE;
            }
        } else {
            statusCode = MrcpResponse.STATUS_MANDATORY_HEADER_MISSING;
        }

        return session.createResponse(statusCode, requestState);
    }

    /* (non-Javadoc)
     * @see org.mrcp4j.server.provider.SpeechSynthRequestHandler#stop(org.mrcp4j.message.request.StopRequest, org.mrcp4j.server.MrcpSession)
     */
    public synchronized MrcpResponse stop(StopRequest request, MrcpSession session) {
        MrcpRequestState requestState = MrcpRequestState.COMPLETE;
        short statusCode = -1;
        _rtpChannel.stopPlayback();
        statusCode = MrcpResponse.STATUS_SUCCESS;

        //TODO: set Active-Request-Id-List header

        return session.createResponse(statusCode, requestState);
    }

    /* (non-Javadoc)
     * @see org.mrcp4j.server.provider.SpeechSynthRequestHandler#pause(org.mrcp4j.message.request.MrcpRequestFactory.UnimplementedRequest, org.mrcp4j.server.MrcpSession)
     */
    public synchronized MrcpResponse pause(UnimplementedRequest request, MrcpSession session) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.mrcp4j.server.provider.SpeechSynthRequestHandler#resume(org.mrcp4j.message.request.MrcpRequestFactory.UnimplementedRequest, org.mrcp4j.server.MrcpSession)
     */
    public synchronized MrcpResponse resume(UnimplementedRequest request, MrcpSession session) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.mrcp4j.server.provider.SpeechSynthRequestHandler#bargeInOccurred(org.mrcp4j.message.request.MrcpRequestFactory.UnimplementedRequest, org.mrcp4j.server.MrcpSession)
     */
    public synchronized MrcpResponse bargeInOccurred(UnimplementedRequest request, MrcpSession session) {
        MrcpRequestState requestState = MrcpRequestState.COMPLETE;
        short statusCode = -1;
        _rtpChannel.stopPlayback();
        statusCode = MrcpResponse.STATUS_SUCCESS;

        //TODO: set Active-Request-Id-List header

        return session.createResponse(statusCode, requestState);
    }

    /* (non-Javadoc)
     * @see org.mrcp4j.server.provider.SpeechSynthRequestHandler#control(org.mrcp4j.message.request.MrcpRequestFactory.UnimplementedRequest, org.mrcp4j.server.MrcpSession)
     */
    public synchronized MrcpResponse control(UnimplementedRequest request, MrcpSession session) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.mrcp4j.server.provider.SpeechSynthRequestHandler#defineLexicon(org.mrcp4j.message.request.MrcpRequestFactory.UnimplementedRequest, org.mrcp4j.server.MrcpSession)
     */
    public synchronized MrcpResponse defineLexicon(UnimplementedRequest request, MrcpSession session) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * TODOC
     *
     */
    private static class PromptGenerator {

        //private String _channelID;
        private File _promptDir;
        private Voice _voice;

        public PromptGenerator(String channelID, File basePromptDir) {
            //_channelID = channelID;
            Validate.isTrue(basePromptDir.isDirectory(), "basePromptDir parameter was not a directory: ", basePromptDir);
            _promptDir = new File(basePromptDir, channelID);
            if (!_promptDir.mkdir()) {
                throw new IllegalArgumentException("Specified directory not valid: " + _promptDir.getAbsolutePath());
            }
            
            String voiceName = "kevin16";
            VoiceManager voiceManager = VoiceManager.getInstance();
            _voice = voiceManager.getVoice(voiceName);

            if (_voice == null) {
                throw new RuntimeException("No tts voice found!");
            }
            _voice.allocate();
        }

        public synchronized File generatePrompt(String text) {
            String promptName = Long.toString(System.currentTimeMillis());
            File promptFile = new File(_promptDir, promptName);
            AudioPlayer ap = new SingleFileAudioPlayer(promptFile.getAbsolutePath(), AudioFileFormat.Type.WAVE);
            _voice.setAudioPlayer(ap);
            _voice.speak(text);
            ap.close();
            _voice.setAudioPlayer(null);
            promptFile = new File(_promptDir, promptName + ".wav");
            if (!promptFile.exists()) {
                throw new RuntimeException("generated prompt file does not exist!");
            }
            return promptFile;
        }
        
    }

}
