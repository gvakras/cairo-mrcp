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

import org.speechforge.cairo.server.resource.ResourceChannel;
import org.speechforge.cairo.server.resource.ResourceImpl;
import org.speechforge.cairo.server.resource.ResourceMediaStream;
import org.speechforge.cairo.server.resource.ResourceMessage;
import org.speechforge.cairo.server.resource.ResourceServer;
import org.speechforge.cairo.server.rtp.RTPConsumer;

import java.awt.Toolkit;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
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
import org.mrcp4j.message.header.MrcpHeaderName;
import org.mrcp4j.message.request.MrcpRequest;

/**
 * TODOC
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 */
public class BargeInClient implements MrcpEventListener {

    private static Logger _logger = Logger.getLogger(BargeInClient.class);

    private static final boolean BEEP = false;

    private static Toolkit _toolkit = BEEP ? Toolkit.getDefaultToolkit() : null;

    private MrcpChannel _ttsChannel;
    private MrcpChannel _recogChannel;

    /**
     * TODOC
     * @param ttsChannel 
     * @param recogChannel 
     */
    public BargeInClient(MrcpChannel ttsChannel, MrcpChannel recogChannel) {
        _ttsChannel = ttsChannel;
        _ttsChannel.addEventListener(this);
        _recogChannel = recogChannel;
        _recogChannel.addEventListener(this);
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

    private void ttsEventReceived(MrcpEvent event) {
        if (BEEP) {
            _toolkit.beep();
        }

        if (MrcpEventName.SPEAK_COMPLETE.equals(event.getEventName())) {
            try {
                sendStartInputTimersRequest();
            } catch (Exception e) {
                _logger.warn(e, e);
            }
        }
        
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
                _logger.warn(e, e);
            }
        } else if (MrcpEventName.RECOGNITION_COMPLETE.equals(eventName)) {
            System.exit(0);
        }
    }

    private MrcpRequestState sendStartInputTimersRequest()
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
     * TODOC
     * @param prompt
     * @param grammarUrl
     * @return
     * @throws IOException
     * @throws MrcpInvocationException
     * @throws InterruptedException
     */
    public MrcpRequestState playAndRecognize(String prompt, URL grammarUrl)
      throws IOException, MrcpInvocationException, InterruptedException {

        // recog request
        MrcpRequest request = _recogChannel.createRequest(MrcpMethodName.RECOGNIZE);
        request.addHeader(MrcpHeaderName.START_INPUT_TIMERS.constructHeader(Boolean.FALSE));
        request.setContent("application/jsgf", null, grammarUrl);
        MrcpResponse response = _recogChannel.sendRequest(request);

        if (_logger.isDebugEnabled()) {
            _logger.debug("MRCP response received:\n" + response.toString());
        }

        if (response.getRequestState().equals(MrcpRequestState.COMPLETE)) {
            throw new RuntimeException("Recognition failed to start!");
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


////////////////////////////////////
//  static methods
////////////////////////////////////

    private static ResourceMessage constructResourceMessage(int localRtpPort) throws UnknownHostException {
        ResourceMessage message = new ResourceMessage();

        List<ResourceChannel> channels = new ArrayList<ResourceChannel>();

        ResourceChannel channel = new ResourceChannel();
        channel.setResourceType(MrcpResourceType.SPEECHSYNTH);
        channels.add(channel);

        channel = new ResourceChannel();
        channel.setResourceType(MrcpResourceType.SPEECHRECOG);
        channels.add(channel);

        message.setChannels(channels);

        ResourceMediaStream stream = new ResourceMediaStream();
        stream.setHost(InetAddress.getLocalHost().getHostName());
        stream.setPort(localRtpPort);
        message.setMediaStream(stream);

        return message;
    }

    private static Options getOptions() {

        Options options = ResourceImpl.getOptions();

//        Option option = new Option("b", "beep");
//        options.addOption(option);

        return options;
    }


////////////////////////////////////
// main method
////////////////////////////////////

    public static void main(String[] args) throws Exception {

        CommandLineParser parser = new GnuParser();
        Options options = getOptions();
        CommandLine line = parser.parse(options, args, true);
        args = line.getArgs();
        
        if (args.length != 3 || line.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("BargeInClient [options] <grammar-URL> <prompt-text> <local-rtp-port>", options);
            return;
        }

        URL grammarUrl = new URL(args[0]);
        String prompt = args[1];
        
        int localRtpPort = -1;

        try {
            localRtpPort = Integer.parseInt(args[2]);
        } catch (Exception e) {
            _logger.debug(e, e);
        }

        if (localRtpPort < 0 || localRtpPort >= RTPConsumer.TCP_PORT_MAX || localRtpPort % 2 != 0) {
            throw new Exception("Improper format for 3rd command line argument <local-rtp-port>," +
                " should be even integer between 0 and " + RTPConsumer.TCP_PORT_MAX);
        }

        // lookup resource server
        InetAddress rserverHost = line.hasOption(ResourceImpl.RSERVERHOST_OPTION) ?
            InetAddress.getByName(line.getOptionValue(ResourceImpl.RSERVERHOST_OPTION)) : InetAddress.getLocalHost();
        String url = "rmi://" + rserverHost.getHostAddress() + '/' + ResourceServer.NAME;
        _logger.info("looking up: " + url);
        ResourceServer resourceServer = (ResourceServer) Naming.lookup(url);

        ResourceMessage message = constructResourceMessage(localRtpPort);
        message = resourceServer.invite(message);
        
        int remoteRtpPort = message.getMediaStream().getPort();

        _logger.debug("Starting NativeMediaClient...");
        NativeMediaClient mediaClient = new NativeMediaClient(localRtpPort, rserverHost, remoteRtpPort);
        mediaClient.startTransmit();

        String protocol = MrcpProvider.PROTOCOL_TCP_MRCPv2;
        MrcpFactory factory = MrcpFactory.newInstance();
        MrcpProvider provider = factory.createProvider();
        
        int i = 0;

        ResourceChannel channel = message.getChannels().get(i++);
        assert (channel.getResourceType() == MrcpResourceType.SPEECHSYNTH) : channel.getResourceType();
        MrcpChannel ttsChannel = provider.createChannel(channel.getChannelID(), rserverHost, channel.getMrcpPort(), protocol);

        channel = message.getChannels().get(i++);
        assert (channel.getResourceType() == MrcpResourceType.SPEECHRECOG) : channel.getResourceType();
        MrcpChannel recogChannel = provider.createChannel(channel.getChannelID(), rserverHost, channel.getMrcpPort(), protocol);

        BargeInClient client = new BargeInClient(ttsChannel, recogChannel);

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
            _logger.warn(e, e);
            System.exit(1);
        }
    }

}
