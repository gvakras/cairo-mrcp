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
package org.speechforge.cairo.demo.tts;

import org.speechforge.cairo.demo.util.DemoSipAgent;
import org.speechforge.cairo.demo.util.NativeMediaClient;
import org.speechforge.cairo.server.resource.ResourceImpl;
import org.speechforge.cairo.server.rtp.RTPConsumer;
import org.speechforge.cairo.util.sip.SipAgent;
import org.speechforge.cairo.util.sip.SdpMessage;
import org.speechforge.cairo.util.sip.SipSession;

import java.awt.Toolkit;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Vector;

import javax.sdp.MediaDescription;
import javax.sdp.SdpException;

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
import org.mrcp4j.message.header.IllegalValueException;
import org.mrcp4j.message.request.MrcpRequest;

/**
 * Demo MRCPv2 client application that utilizes a {@code speechsynth} resource to play a TTS prompt.
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 */
public class SpeechSynthClient implements MrcpEventListener {

    private static Logger _logger = Logger.getLogger(SpeechSynthClient.class);

    private static final String BEEP_OPTION = "beep";
    private static final String REPETITIONS_OPTION = "reps";

    private static boolean _beep = false;
    private static Toolkit _toolkit = null;
    private static int _repetitions = 1;

    private MrcpChannel _ttsChannel;
    private int _rep = 1;
    
    private static int _myPort = 5070;
    private static String _host = null;
    private static int _peerPort = 5060;
    private static String _mySipAddress ="sip:speechSynthClient@speechforge.org";
    private static String _cairoSipAddress="sip:cairo@speechforge.org";

    /**
     * TODOC
     * @param ttsChannel 
     * @param recogChannel 
     * @param recordChannel 
     */
    public SpeechSynthClient(MrcpChannel ttsChannel) {
        _ttsChannel = ttsChannel;
        _ttsChannel.addEventListener(this);
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

            default:
                _logger.warn("Unexpected value for event resource type!");
                break;
            }
        } catch (IllegalValueException e) {
            _logger.warn("Illegal value for event resource type!", e);
        }
    }

    private void ttsEventReceived(MrcpEvent event) {
        if (event.getEventName().equals(MrcpEventName.SPEAK_COMPLETE) && _rep++ >= _repetitions) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                _logger.debug("InterruptedException encountered!", e);
            }
            System.exit(0);
        }
    }

    public MrcpRequestState playPrompt(String promptText)
      throws IOException, MrcpInvocationException, InterruptedException {

        // speak request
        MrcpRequest request = _ttsChannel.createRequest(MrcpMethodName.SPEAK);
        request.setContent("text/plain", null, promptText);
        MrcpResponse response = _ttsChannel.sendRequest(request);

        if (_beep) {
            _toolkit.beep();
        }

        if (_logger.isDebugEnabled()) {
            _logger.debug("MRCP response received:\n" + response.toString());
        }

        return response.getRequestState();
    }


////////////////////////////////////
// static methods
////////////////////////////////////
    private static SdpMessage constructResourceMessage(int localRtpPort) throws UnknownHostException, SdpException {
        SdpMessage sdpMessage = SdpMessage.createNewSdpSessionMessage(_mySipAddress, _host, "The session Name");
        MediaDescription rtpChannel = SdpMessage.createRtpChannelRequest(localRtpPort);
        MediaDescription mrcpChannel = SdpMessage.createMrcpChannelRequest(MrcpResourceType.SPEECHSYNTH);
        Vector v = new Vector();
        v.add(mrcpChannel);
        v.add(rtpChannel);
        sdpMessage.getSessionDescription().setMediaDescriptions(v);
        return sdpMessage;
    }
    
    
    private static Options getOptions() {
        Options options = ResourceImpl.getOptions();

        Option option = new Option(BEEP_OPTION, "play response/event timing beep");
        options.addOption(option);

        option = new Option(REPETITIONS_OPTION, true, "number of times to repeat the TTS prompt");
        option.setArgName("repetitions");
        options.addOption(option);

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
        
        if (args.length != 2 || line.hasOption(ResourceImpl.HELP_OPTION)) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("SpeechSynthClient [options] <local-rtp-port> <prompt-text>", options);
            return;
        }

        if (line.hasOption(REPETITIONS_OPTION)) {
            try {
                _repetitions = Integer.parseInt(line.getOptionValue(REPETITIONS_OPTION));
            } catch (NumberFormatException e) {
                _logger.debug("Could not parse repetitions parameter to int!", e);
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("SpeechSynthClient [options] <prompt-text> <local-rtp-port>", options);
                return;
            }
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

        String promptText = args[1];
        InetAddress rserverHost = line.hasOption(ResourceImpl.RSERVERHOST_OPTION) ?
            InetAddress.getByName(line.getOptionValue(ResourceImpl.RSERVERHOST_OPTION)) : InetAddress.getLocalHost();

        try {
            _host = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            _host = "localhost";
        }
        String peerAddress = rserverHost.getHostAddress();

        // Construct a SIP agent to be used to send a SIP Invitation to the ciaro server
        //DemoSipListener listener = new DemoSipListener();
        DemoSipAgent sipAgent = new DemoSipAgent(_mySipAddress, "Synth Client Sip Stack", _myPort, "UDP");

        // Construct the SDP message that will be sent in the SIP invitation
        SdpMessage message = constructResourceMessage(localRtpPort);

        // Send the sip invitation (This method on the demoSipAgent blocks until a response is received or a timeout occurs) 
        _logger.info("Sending a SIP invitation to the cairo server.");
        SdpMessage inviteResponse = sipAgent.sendInviteWithoutProxy(_cairoSipAddress, message, peerAddress, _peerPort);

        if (inviteResponse != null) {
            _logger.info("Received the SIP Response.");

            // Get the MRCP media channels (need the port number and the channelID that are sent
            // back from the server in the response in order to setup the MRCP channel)
            List <MediaDescription> xmitterChans = inviteResponse.getMrcpTransmitterChannels();
            int port = xmitterChans.get(0).getMedia().getMediaPort();
            String channelId = xmitterChans.get(0).getAttribute(SdpMessage.SDP_CHANNEL_ATTR_NAME);

            //Construct the MRCP Channel
            String protocol = MrcpProvider.PROTOCOL_TCP_MRCPv2;
            MrcpFactory factory = MrcpFactory.newInstance();
            MrcpProvider provider = factory.createProvider();
            MrcpChannel ttsChannel = provider.createChannel(channelId, rserverHost, port, protocol);

            //Setup a media client to receive and play the sythesized voice data streamed over the RTP channel
            _logger.debug("Starting NativeMediaClient for receive only...");
            NativeMediaClient mediaClient = new NativeMediaClient(localRtpPort); 

            SpeechSynthClient client = new SpeechSynthClient(ttsChannel);

            // Use the MRCP channel to instruct the cairo server to sythesize voice data and send it over the
            // RTP channel as specified in teh SIP invitation
            try {
                for (int i=0; i < _repetitions; i++) {
                    client.playPrompt(promptText);
                }
            } catch (Exception e){
                if (e instanceof MrcpInvocationException) {
                    MrcpResponse response = ((MrcpInvocationException) e).getResponse();
                    _logger.warn("MRCP response received:\n" + response);
                }
                _logger.warn(e, e);
                sipAgent.dispose();
                System.exit(1);
            }

        } else {
            //Invitation Timeout
            _logger.info("Sip Invitation timed out.  Is server running?");
        }
        
        //Dispose of SIP Agent
        sipAgent.dispose();
        //System.exit(0);
    }

}
