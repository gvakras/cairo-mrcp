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
package com.onomatopia.cairo.server.tts;

import com.onomatopia.cairo.exception.UnsupportedHeaderException;
import com.onomatopia.cairo.server.MrcpGenericChannel;

import java.io.File;
import java.io.IOException;

import javax.media.rtp.InvalidSessionAddressException;


import org.mrcp4j.MrcpRequestState;
import org.mrcp4j.message.MrcpResponse;
import org.mrcp4j.message.header.IllegalValueException;
import org.mrcp4j.message.header.MrcpHeader;
import org.mrcp4j.message.request.StopRequest;
import org.mrcp4j.message.request.MrcpRequestFactory.UnimplementedRequest;
import org.mrcp4j.server.MrcpSession;
import org.mrcp4j.server.provider.SpeechSynthRequestHandler;

/**
 * MRCP channel implementation for "speechsynth" resource.
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 */
public class MrcpSpeechSynthChannel extends MrcpGenericChannel implements SpeechSynthRequestHandler {

//    private static short IDLE = 0;
//    private static short SPEAKING = 1;
//    private static short PAUSED = 2;
//
//    volatile short _state = IDLE;

    private PromptGenerator _promptGenerator;
    private RTPSpeechSynthChannel _rtpChannel;
    private File _promptDir;

    /**
     * TODOC
     * @param channelID 
     * @param basePromptDir 
     * @param rtpChannel 
     * @throws IllegalArgumentException 
     */
    public MrcpSpeechSynthChannel(String channelID, RTPSpeechSynthChannel rtpChannel, File basePromptDir) throws IllegalArgumentException {
        if (basePromptDir == null || !basePromptDir.isDirectory()) {
            throw new IllegalArgumentException("Base prompt directory file specified does not exist or is not a directory: " + basePromptDir);
        }

        _promptDir = new File(basePromptDir, channelID);
        if (!_promptDir.mkdir()) {
            throw new RuntimeException("Could not make prompt directory: " + _promptDir.getAbsolutePath());
        }

        _rtpChannel = rtpChannel;
        _promptGenerator = new PromptGenerator();
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
                String text = request.getContent();
                try {
                    File promptFile = _promptGenerator.generatePrompt(text, _promptDir);
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

    /* (non-Javadoc)
     * @see com.onomatopia.cairo.server.MrcpGenericChannel#validateParam(org.mrcp4j.message.header.MrcpHeader)
     */
    @Override
    protected boolean validateParam(MrcpHeader header) throws UnsupportedHeaderException, IllegalValueException {
        throw new UnsupportedHeaderException();
    }

}
