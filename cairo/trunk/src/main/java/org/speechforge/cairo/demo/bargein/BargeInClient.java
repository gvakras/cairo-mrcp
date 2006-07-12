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
package org.speechforge.cairo.demo.bargein;

import org.speechforge.cairo.demo.util.NativeMediaClient;

import org.speechforge.cairo.server.resource.ReceiverResource;
import org.speechforge.cairo.server.resource.ResourceChannel;
import org.speechforge.cairo.server.resource.ResourceMediaStream;
import org.speechforge.cairo.server.resource.ResourceMessage;
import org.speechforge.cairo.server.resource.ResourceServer;

import java.awt.Toolkit;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.rmi.Naming;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.mrcp4j.MrcpEventName;
import org.mrcp4j.MrcpMethodName;
import org.mrcp4j.MrcpRequestState;
import org.mrcp4j.MrcpResourceType;
import org.mrcp4j.client.MrcpChannel;
import org.mrcp4j.client.MrcpEventListener;
import org.mrcp4j.client.MrcpFactory;
import org.mrcp4j.client.MrcpInvocationException;
import org.mrcp4j.client.MrcpProvider;
import org.mrcp4j.message.MrcpEvent;
import org.mrcp4j.message.MrcpResponse;
import org.mrcp4j.message.header.IllegalValueException;
import org.mrcp4j.message.request.MrcpRequest;

/**
 * TODOC
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 */
public class BargeInClient implements MrcpEventListener {

    private static Logger _logger = Logger.getLogger(BargeInClient.class);

    private static final boolean RECORD = false;
    private static final boolean BEEP = false;

    private static Toolkit _toolkit = BEEP ? Toolkit.getDefaultToolkit() : null;

    private MrcpChannel _ttsChannel;
    private MrcpChannel _recogChannel;
    private MrcpChannel _recordChannel;

    /**
     * TODOC
     * @param ttsChannel 
     * @param recogChannel 
     * @param recordChannel 
     */
    public BargeInClient(MrcpChannel ttsChannel, MrcpChannel recogChannel, MrcpChannel recordChannel) {
        _ttsChannel = ttsChannel;
        _ttsChannel.addEventListener(this);
        _recogChannel = recogChannel;
        _recogChannel.addEventListener(this);
        if (recordChannel != null) {
            _recordChannel = recordChannel;
            _recordChannel.addEventListener(this);
        }
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

            case RECORDER:
                recordEventReceived(event);
                break;

            default:
                break;
            }
        } catch (IllegalValueException e) {
            e.printStackTrace();
        }
    }


    /**
     * TODOC
     * @param event
     */
    private void ttsEventReceived(MrcpEvent event) {
        // TODO Auto-generated method stub
        
    }

    private void recogEventReceived(MrcpEvent event) {
        if (BEEP) {
            _toolkit.beep();
        }
        
        MrcpEventName eventName = event.getEventName();

        if (MrcpEventName.START_OF_INPUT.equals(eventName)) {
            try {
                sendBargeinRequest();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (MrcpEventName.RECOGNITION_COMPLETE.equals(eventName)) {
            if (_recordChannel != null) try {
                MrcpRequest request = _recordChannel.createRequest(MrcpMethodName.STOP);
                MrcpResponse response = _recordChannel.sendRequest(request);
                if (_logger.isDebugEnabled()) {
                    _logger.debug("MRCP response received:\n" + response.toString());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.exit(0);
        }
        
    }

    /**
     * TODOC
     * @param event
     */
    private void recordEventReceived(MrcpEvent event) {
        // TODO Auto-generated method stub
        
    }

    public MrcpRequestState playAndRecognize(String prompt, URL grammarUrl)
      throws IOException, MrcpInvocationException, InterruptedException {

        // recog request
        MrcpRequest request = _recogChannel.createRequest(MrcpMethodName.RECOGNIZE);
        request.setContent("application/jsgf", null, grammarUrl);
        MrcpResponse response = _recogChannel.sendRequest(request);

        if (_logger.isDebugEnabled()) {
            _logger.debug("MRCP response received:\n" + response.toString());
        }
        
        if (response.getRequestState().equals(MrcpRequestState.COMPLETE)) {
            throw new RuntimeException("Recognition failed to start!");
        }

        if (_recordChannel != null) {
            // record request
            request = _recordChannel.createRequest(MrcpMethodName.RECORD);
            response = _recordChannel.sendRequest(request);
        }

        if (_logger.isDebugEnabled()) {
            _logger.debug("MRCP response received:\n" + response.toString());
        }

        // speak request
        request = _ttsChannel.createRequest(MrcpMethodName.SPEAK);
        request.setContent("text/plain", null, prompt);
        response = _ttsChannel.sendRequest(request);

        if (BEEP) {
            _toolkit.beep();
        }

        if (_logger.isDebugEnabled()) {
            _logger.debug("MRCP response received:\n" + response.toString());
        }

        return response.getRequestState();
    }

    public MrcpRequestState sendBargeinRequest()
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

    private static ResourceMessage constructResourceMessage(int localRtpPort) {
        ResourceMessage message = new ResourceMessage();

        List<ResourceChannel> channels = new ArrayList<ResourceChannel>();

        ResourceChannel channel = new ResourceChannel();
        channel.setResourceType(MrcpResourceType.SPEECHSYNTH);
        channels.add(channel);

        channel = new ResourceChannel();
        channel.setResourceType(MrcpResourceType.SPEECHRECOG);
        channels.add(channel);

        if (RECORD) {
            channel = new ResourceChannel();
            channel.setResourceType(MrcpResourceType.RECORDER);
            channels.add(channel);
        }

        message.setChannels(channels);

        ResourceMediaStream stream = new ResourceMediaStream();
        stream.setPort(localRtpPort);
        message.setMediaStream(stream);

        return message;
    }

    /**
     * TODOC
     * @param args
     * @throws Exception 
     */
    public static void main(String[] args) throws Exception {
        //URL grammarUrl = new URL("file:///work/source/zanzibar/grammar/example.gram");
        URL grammarUrl = new URL(args[0]);

        int localRtpPort = 42046;

        String prompt = "You can start speaking any time.  Would you like to hear the weather,"
            + " get sports news or hear a stock quote?";

        // lookup resource server
        InetAddress host = InetAddress.getLocalHost();
        String url = "rmi://" + host.getHostAddress() + '/' + ResourceServer.NAME;
        if (_logger.isDebugEnabled()) {
            _logger.debug("looking up: " + url);
        }

        ResourceServer resourceServer = (ResourceServer) Naming.lookup(url);

        ResourceMessage message = constructResourceMessage(localRtpPort);
        message = resourceServer.invite(message);
        
        int remoteRtpPort = message.getMediaStream().getPort();
        //TODO: get remote host address (assume localhost for now)

        _logger.debug("Starting NativeMediaClient...");
        NativeMediaClient mediaClient = new NativeMediaClient(localRtpPort, host, remoteRtpPort);
        mediaClient.startTransmit();

        String protocol = MrcpProvider.PROTOCOL_TCP_MRCPv2;
        MrcpFactory factory = MrcpFactory.newInstance();
        MrcpProvider provider = factory.createProvider();
        
        int i = 0;

        ResourceChannel channel = message.getChannels().get(i++);
        assert (channel.getResourceType() == MrcpResourceType.SPEECHSYNTH) : channel.getResourceType();
        MrcpChannel ttsChannel = provider.createChannel(channel.getChannelID(), host, channel.getPort(), protocol);

        channel = message.getChannels().get(i++);
        assert (channel.getResourceType() == MrcpResourceType.SPEECHRECOG) : channel.getResourceType();
        MrcpChannel recogChannel = provider.createChannel(channel.getChannelID(), host, channel.getPort(), protocol);

        MrcpChannel recordChannel = null;
        if (RECORD) {
            channel = message.getChannels().get(i++);
            assert (channel.getResourceType() == MrcpResourceType.RECORDER) : channel.getResourceType();
            recordChannel = provider.createChannel(channel.getChannelID(), host, channel.getPort(), protocol);
        }

        BargeInClient client = new BargeInClient(ttsChannel, recogChannel, recordChannel);

        try {
            client.playAndRecognize(prompt, grammarUrl);
            if (BEEP) {
                _toolkit.beep();
            }
        } catch (Exception e){
            if (e instanceof MrcpInvocationException) {
                MrcpResponse response = ((MrcpInvocationException) e).getResponse();
                if (_logger.isDebugEnabled()) {
                    _logger.debug("MRCP response received:\n" + response.toString());
                }
            }
            e.printStackTrace();
            System.exit(1);
        }
    }

}
