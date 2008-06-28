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
package org.speechforge.cairo.demo.recog;

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
import org.apache.commons.cli.Option;
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
import org.mrcp4j.message.header.CompletionCause;
import org.mrcp4j.message.header.IllegalValueException;
import org.mrcp4j.message.header.MrcpHeader;
import org.mrcp4j.message.header.MrcpHeaderName;
import org.mrcp4j.message.request.MrcpRequest;

/**
 * Demo MRCPv2 client application that utilizes a {@code speechrecog} resource to perform
 * speech recognition on microphone input.
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 */
public class RecognitionClient implements MrcpEventListener {

    private static Logger _logger = Logger.getLogger(RecognitionClient.class);

    private static final String BEEP_OPTION = "beep";

    private static boolean _beep = false;
    private static Toolkit _toolkit = null;

    private MrcpChannel _recogChannel;
    private MrcpEvent _mrcpEvent;

    /**
     * TODOC
     * @param recogChannel 
     */
    public RecognitionClient(MrcpChannel recogChannel) {
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

    private void recogEventReceived(MrcpEvent event) {

        MrcpEventName eventName = event.getEventName();

        if (MrcpEventName.RECOGNITION_COMPLETE.equals(eventName)) {
            if (_beep) {
                _toolkit.beep();
            }
            synchronized (this) {
                _mrcpEvent = event;
                this.notifyAll();
            }
        }
    }

    public synchronized String doRecognize(URL grammarUrl, String examplePhrase)
      throws IOException, MrcpInvocationException, InterruptedException, IllegalValueException {

        _mrcpEvent = null;

        // recog request
        MrcpRequest request = _recogChannel.createRequest(MrcpMethodName.RECOGNIZE);
        request.addHeader(MrcpHeaderName.NO_INPUT_TIMEOUT.constructHeader(new Long(30000)));
        request.setContent("application/jsgf", null, grammarUrl);
        MrcpResponse response = _recogChannel.sendRequest(request);

        if (_beep) {
            _toolkit.beep();
        }

        if (_logger.isDebugEnabled()) {
            _logger.debug("MRCP response received:\n" + response.toString());
        }
        
        if (response.getRequestState().equals(MrcpRequestState.COMPLETE)) {
            throw new RuntimeException("Recognition failed to start!");
        }
        
        if (_logger.isInfoEnabled()) {
            if (examplePhrase == null) {
                _logger.info("\nStart speaking now...");
            } else {
                _logger.info("\nStart speaking now... (e.g. \"" + examplePhrase + "\")");
            }
        }

        while (_mrcpEvent == null) {
            this.wait();
        }

        MrcpHeader completionCauseHeader = _mrcpEvent.getHeader(MrcpHeaderName.COMPLETION_CAUSE);
        CompletionCause completionCause = (CompletionCause) completionCauseHeader.getValueObject();

        return (completionCause.getCauseCode() == 0) ? _mrcpEvent.getContent() : null ;

    }


////////////////////////////////////
//static methods
////////////////////////////////////

    private static ResourceMessage constructResourceMessage(int localRtpPort) throws UnknownHostException {
        ResourceMessage message = new ResourceMessage();

        List<ResourceChannel> channels = new ArrayList<ResourceChannel>();

        ResourceChannel channel = new ResourceChannel();
        channel.setResourceType(MrcpResourceType.SPEECHRECOG);
        channels.add(channel);

        message.setChannels(channels);

        ResourceMediaStream stream = new ResourceMediaStream();
        stream.setHost(InetAddress.getLocalHost().getHostName());
        stream.setPort(localRtpPort);
        message.setMediaStream(stream);

        return message;
    }

    public static Options getOptions() {
        Options options = ResourceImpl.getOptions();

        Option option = new Option(BEEP_OPTION, "play response/event timing beep");
        options.addOption(option);

        return options;
    }


////////////////////////////////////
//  main method
////////////////////////////////////

    public static void main(String[] args) throws Exception {

        CommandLineParser parser = new GnuParser();
        Options options = getOptions();
        CommandLine line = parser.parse(options, args, true);
        args = line.getArgs();

        if (args.length < 2 || args.length > 3 || line.hasOption(ResourceImpl.HELP_OPTION)) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("RecognitionClient [options] <local-rtp-port> <grammar-URL> <example-phrase>", options);
            return;
        }

        _beep = line.hasOption(BEEP_OPTION);
        if (_beep) {
            _toolkit = Toolkit.getDefaultToolkit();
        }

        int localRtpPort = -1;

        try {
            localRtpPort = Integer.parseInt(args[0]);
        } catch (Exception e) {
            _logger.debug(e, e);
        }

        if (localRtpPort < 0 || localRtpPort >= RTPConsumer.TCP_PORT_MAX || localRtpPort % 2 != 0) {
            throw new Exception("Improper format for first command line argument <local-rtp-port>," +
                " should be even integer between 0 and " + RTPConsumer.TCP_PORT_MAX);
        }

        URL grammarUrl = new URL(args[1]);

        String examplePhrase = (args.length > 2) ? args[2] : null;

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

        ResourceChannel channel = message.getChannels().get(0);
        assert (channel.getResourceType() == MrcpResourceType.SPEECHRECOG) : channel.getResourceType();
        MrcpChannel recogChannel = provider.createChannel(channel.getChannelID(), rserverHost, channel.getMrcpPort(), protocol);

        RecognitionClient client = new RecognitionClient(recogChannel);

        try {
            String result = client.doRecognize(grammarUrl, examplePhrase);
            if (_logger.isInfoEnabled()) {
                StringBuilder sb = new StringBuilder();
                sb.append("\n**************************************************************");
                sb.append("\nRecognition result: ").append(result);
                sb.append("\n**************************************************************\n");
                _logger.info(sb);
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

        System.exit(0);
    }

}
