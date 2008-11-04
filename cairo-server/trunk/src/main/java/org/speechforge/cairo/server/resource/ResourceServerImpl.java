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
package org.speechforge.cairo.server.resource;


import org.speechforge.cairo.sip.ResourceUnavailableException;
import org.speechforge.cairo.sip.SipAgent;
import org.speechforge.cairo.sip.SdpMessage;
import org.speechforge.cairo.sip.SessionListener;
import org.speechforge.cairo.sip.SipResource;
import org.speechforge.cairo.sip.SipSession;

import java.awt.Toolkit;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import javax.sdp.MediaDescription;
import javax.sdp.SdpException;
import javax.sip.SipException;
import javax.sip.TimeoutEvent;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.log4j.Logger;
import org.mrcp4j.MrcpResourceType;

/**
 * Implements a {@link org.speechforge.cairo.server.resource.ResourceServer} that can be utilized by MRCPv2
 * clients for establishing and managing connections to MRCPv2 resource implementations.
 * 
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 */
public class ResourceServerImpl implements SessionListener {

    public static final String NAME = "ResourceServer";

    private static Logger _logger = Logger.getLogger(ResourceServerImpl.class);


    private static final String SIPPORT_OPTION = "sipPort";
    private static final String SIPTRANSPORT_OPTION = "sipTransport";
  
    private long _channelID = System.currentTimeMillis();

    private ResourceRegistry _registryImpl;

    private SipAgent _ua;

    private String cairoSipAddress = "sip:cairo@speechforge.org";

    /**
     * TODOC
     * 
     * @param registryImpl
     * @throws RemoteException
     * @throws SipException
     */
    public ResourceServerImpl(ResourceRegistry registryImpl, int sipPort, String sipTransport) throws RemoteException, SipException {
        super();
        String hostIpAddress = null;
        try {
            InetAddress addr = InetAddress.getLocalHost();
            hostIpAddress = addr.getHostAddress();
            //host = addr.getCanonicalHostName();
        } catch (UnknownHostException e) {
            hostIpAddress = "127.0.0.1";
            _logger.debug(e, e);
            e.printStackTrace();
        }
        if (sipPort == 0) sipPort = 5050;
        if (sipTransport == null) sipTransport = "tcp";
        cairoSipAddress = "sip:cairo@"+hostIpAddress;
        _ua = new SipAgent(this, cairoSipAddress, "Cairo SIP Stack", sipPort, sipTransport);

        _registryImpl = registryImpl;
    }

    /**
     * TODOC
     * 
     * @param port
     * @param registryImpl
     * @throws RemoteException
     */
    public ResourceServerImpl(int port, ResourceRegistryImpl registryImpl) throws RemoteException {
        _registryImpl = registryImpl;
    }

    private synchronized String getNextChannelID() { // TODO: convert from synchronized to atomic
        return Long.toHexString(_channelID++);
    }

    /**
     * Invite.
     * 
     * @param request
     *            the invite request
     * 
     * @return the invite response
     * 
     * @throws ResourceUnavailableException
     *             the resource unavailable exception
     * @throws RemoteException
     *             the remote exception
     * @throws SdpException
     *             the sdp exception
     */
    private SdpMessage invite(SdpMessage request, SipSession session) throws ResourceUnavailableException, RemoteException,
            SdpException {

        // determine if there receivers and/or transmitter channel requests in the invite
        // and preprocess the message so that it can be sent back as a response to the inviter
        // (i.e. set the channel and setup attributes).
        boolean receiver = false;
        boolean transmitter = false;
        try {
            for (MediaDescription md : request.getMrcpReceiverChannels()) {
                String channelID = getNextChannelID();
                String chanid = channelID + '@' + MrcpResourceType.SPEECHRECOG.toString();
                md.setAttribute("channel", chanid);
                md.setAttribute("setup", "passive");
                receiver = true;
            }
            for (MediaDescription md : request.getMrcpTransmitterChannels()) {
                String channelID = getNextChannelID();
                String chanid = channelID + '@' + MrcpResourceType.SPEECHSYNTH.toString();
                md.setAttribute("channel", chanid);
                md.setAttribute("setup", "passive");
                transmitter = true;
            }
        } catch (SdpException e) {
            _logger.debug(e, e);
            throw e;
        }

        // process the invitation (transmiiiter and/or receiver)
        if (transmitter) {
            Resource resource;
            try {
                resource = _registryImpl.getResource(Resource.Type.TRANSMITTER);
            } catch (org.speechforge.cairo.exception.ResourceUnavailableException e) {
                e.printStackTrace();
                throw new org.speechforge.cairo.sip.ResourceUnavailableException("Could not get a transmitter resource");
            }
            request = resource.invite(request, session.getId());
            session.getResources().add(resource);
        }

        if (receiver) {
            Resource resource;
            try {
                resource = _registryImpl.getResource(Resource.Type.RECEIVER);
            } catch (org.speechforge.cairo.exception.ResourceUnavailableException e) {
                e.printStackTrace();
                throw new org.speechforge.cairo.sip.ResourceUnavailableException("Could not get a receiver resource");
            }
            request = resource.invite(request, session.getId());
            session.getResources().add(resource);
        } // TODO: catch exception and release transmitter resources

        // post process the message
        // - remove the resource attribute
        // TODO: change the host adresss on a per channel basis (in case the resources are distributed across a network)
        for (MediaDescription md : request.getMrcpChannels()) {
            md.removeAttribute("resource");
        }
        // message.getSessionDescription().getConnection().setAddress(host);

        return request;
    }

    public void processByeRequest(SipSession session) throws RemoteException, InterruptedException {
        for (SipResource r : session.getResources()) {
            r.bye(session.getId());
        }
    }

    public SdpMessage processInviteRequest(SdpMessage request, SipSession session) throws SdpException,
            ResourceUnavailableException, RemoteException {
        SdpMessage m = invite(request, session);
        _ua.sendResponse(session, m);
        return m;
    }

    public SdpMessage processInviteResponse(boolean ok, SdpMessage response, SipSession session) {
        // TODO Auto-generated method stub
        return null;
    }
    
    private static Options getOptions() {
        Options options = ResourceImpl.getOptions();

        Option option = new Option(SIPPORT_OPTION, true, "The port the sip agent uses to listen for requests.");
        options.addOption(option);

        option = new Option(SIPTRANSPORT_OPTION, true, "The transport used by the sip agent udp or tcp.");
        options.addOption(option);


        return options;
    }

    /**
     * TODOC
     * 
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        CommandLineParser parser = new GnuParser();
        Options options = getOptions();
        CommandLine line = parser.parse(options, args, true);
        args = line.getArgs();

        System.out.println(args.length+" ->"+ line.toString());
        /*if (args.length < 3 || args.length > 5 || line.hasOption(ResourceImpl.HELP_OPTION)) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("ResourceServerImpl [options] ", options);
            return;
        }*/


        int sipPort = 0;
        String sipTransport = null;
        if (line.hasOption(SIPPORT_OPTION)) {
            String tmp = line.getOptionValue(SIPPORT_OPTION);
            sipPort = Integer.valueOf(tmp);
        }

        if (line.hasOption(SIPTRANSPORT_OPTION)) {
           sipTransport = line.getOptionValue(SIPTRANSPORT_OPTION);
        }
        
        _logger.info("Command line specified sip port: "+sipPort+ " and sip transport: "+ sipTransport);
       
        ResourceRegistryImpl rr = new ResourceRegistryImpl();
        ResourceServerImpl rs = new ResourceServerImpl(rr,sipPort,sipTransport);

        Registry registry = LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
        registry.rebind(ResourceRegistry.NAME, rr);
        // registry.rebind(ResourceServer.NAME, rs);

        _logger.info("Server and registry bound and waiting...");

    }

    public void processTimeout(TimeoutEvent event) {
        // TODO Auto-generated method stub
        
    }
    public void processInfoRequest(SipSession session, String contentType, String contentSubType, String content) {
        // TODO Auto-generated method stub
    }

}
