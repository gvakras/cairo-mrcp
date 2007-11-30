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
package org.speechforge.cairo.util.sip;

import java.net.InetAddress;

import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;

import java.util.Random;

import java.util.Properties;
import java.util.TooManyListenersException;

import javax.sip.ClientTransaction;
import javax.sip.Dialog;

import javax.sip.InvalidArgumentException;
import javax.sip.ListeningPoint;
import javax.sip.ObjectInUseException;
import javax.sip.PeerUnavailableException;
import javax.sip.SipException;
import javax.sip.SipFactory;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.SipStack;
import javax.sip.TransactionUnavailableException;
import javax.sip.TransportNotSupportedException;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.address.URI;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.FromHeader;

import javax.sip.header.HeaderFactory;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.RouteHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.ViaHeader;

import javax.sip.message.MessageFactory;
import javax.sip.message.Request;

import org.apache.log4j.Logger;

/**
 * The SipAgent used by Cairo elements for SIP signaling.
 * 
 * @author Spencer Lord {@literal <}<a href="mailto:slord@users.sourceforge.net">salord@users.sourceforge.net</a>{@literal >}
 */
public class SipAgent {

    private static Logger _logger = Logger.getLogger(SipAgent.class);

    // SIP protocol objects
    static AddressFactory addressFactory;

    static MessageFactory messageFactory;

    static HeaderFactory headerFactory;

    static SipStack sipStack;

    private SipProvider sipProvider;

    private ListeningPoint listeningPoint;

    private SipListener listener;

    private String transport = "udp";

    private int port;

    private String mySipAddress;

    private Address myAddress;

    private Address contactAddress;

    private String guidPrefix;

    static int sipStackLogLevel = 0;

    static String logFileDirectory = "logs/";

    private String sipAddress = "sip:cairo@speechforge.com";

    private String stackName = "SipStack";

    private String hostIpAddress;

    private String host;

    private SessionListener sessionListener;

    private Random random = new Random((new Date()).getTime());

    public SipAgent(SessionListener sessionListener, String mySipAddress) throws SipException {
        this(sessionListener, mySipAddress, "CairoSipStack", 5060, "udp");
    }

    public SipAgent(SessionListener sessionListener, String mySipAddress, String stackName, int port,
            String transport) throws SipException {
        this.sessionListener = sessionListener;
        this.stackName = stackName;
        this.port = port;
        this.transport = transport;
        this.mySipAddress = mySipAddress;
        init();
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getTransport() {
        return transport;
    }

    public String getStackName() {
        return stackName;
    }

    public AddressFactory getAddressFactory() {
        return addressFactory;
    }

    public MessageFactory getMessageFactory() {
        return messageFactory;
    }

    public HeaderFactory getHeaderFactory() {
        return headerFactory;
    }

    public SipStack getSipStack() {
        return sipStack;
    }

    public SipProvider getSipProvider() {
        return sipProvider;
    }

    public ListeningPoint getListeningPoint() {
        return listeningPoint;
    }

    private void init() throws SipException {

        try {
            InetAddress addr = InetAddress.getLocalHost();
            hostIpAddress = addr.getHostAddress();
            host = addr.getCanonicalHostName();
        } catch (UnknownHostException e) {
            hostIpAddress = "127.0.0.1";
            host = "localhost";
            _logger.debug(e, e);
            e.printStackTrace();
        }

        guidPrefix = hostIpAddress + port + System.currentTimeMillis();
        SipFactory sipFactory = null;

        sipFactory = SipFactory.getInstance();
        sipFactory.setPathName("gov.nist");
        Properties properties = new Properties();
        properties.setProperty("javax.sip.STACK_NAME", stackName);

        // The following properties are specific to nist-sip
        // and are not necessarily part of any other jain-sip
        // implementation.
        properties.setProperty("gov.nist.javax.sip.DEBUG_LOG", logFileDirectory + stackName + "debug.txt");
        properties.setProperty("gov.nist.javax.sip.SERVER_LOG", logFileDirectory + stackName + "log.txt");

        // Set to 0 in your production code for max speed.
        // You need 16 for logging traces. 32 for debug + traces.
        // Your code will limp at 32 but it is best for debugging.
        properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", new Integer(sipStackLogLevel).toString());

        try {
            sipStack = sipFactory.createSipStack(properties);
        } catch (PeerUnavailableException e) {
            _logger.debug(e, e);
            throw new SipException("Stack failed to initialize", e);
        }

        try {
            headerFactory = sipFactory.createHeaderFactory();
            addressFactory = sipFactory.createAddressFactory();
            messageFactory = sipFactory.createMessageFactory();
        } catch (SipException e) {
            _logger.debug(e, e);
            throw new SipException("Could not create SIP factories", e);
        }

        try {
            listeningPoint = sipStack.createListeningPoint(hostIpAddress, port, transport);
            sipProvider = sipStack.createSipProvider(listeningPoint);
        } catch (TransportNotSupportedException e) {
            _logger.debug(e, e);
            throw new SipException("Could not create listening point. Transport not supported.", e);
        } catch (InvalidArgumentException e) {
            _logger.debug(e, e);
            throw new SipException("Could not create listening point. Invalid argument.", e);
        } catch (ObjectInUseException e) {
            _logger.debug(e, e);
            throw new SipException("Could not create listening point. Object in use.", e);
        }

        try {
            listener = (SipListener) new SipListenerImpl(this);
            sipProvider.addSipListener(listener);
        } catch (TooManyListenersException e) {
            _logger.debug(e, e);
            throw new SipException("Could not add listener. Too many listeners.", e);
        }

        // create my address (for from headers) and the contact address (for
        // contact header)
        try {
            URI uri = addressFactory.createURI(mySipAddress);
            if (uri.isSipURI() == false) {
                _logger.error("Invalid sip uri: " + mySipAddress);
                throw new SipException("Invalid sip uri: " + mySipAddress);
            }
            myAddress = addressFactory.createAddress(uri);
            // create a contact address (for contact header)
            SipURI contactUri = addressFactory.createSipURI(((SipURI) uri).getUser(), this.hostIpAddress);
            // SipURI contactUrl = addressFactory.createSipURI(from.getName(),
            // host);
            contactUri.setPort(listeningPoint.getPort());
            contactAddress = addressFactory.createAddress(contactUri);
        } catch (ParseException e) {
            _logger.debug(e, e);
            throw new SipException("Could not create contact URI.", e);
        }
    }

    public void dispose() throws ObjectInUseException {
        sipStack.deleteListeningPoint(sipProvider.getListeningPoints()[0]);
        sipProvider.removeSipListener(listener);
        sipStack.deleteSipProvider(sipProvider);
    }

    public void sendInfoRequestWithoutProxy(String to, String peerHost, int peerPort) {
        // TODO: implement getting the info from the server
    }

    public SipSession sendInviteWithoutProxy(String to, SdpMessage message, String peerHost, int peerPort)
            throws SipException {
        SipSession session = null;
        try {

            // create >From Header
            FromHeader fromHeader = headerFactory.createFromHeader(myAddress, getGUID());

            URI toUri = null;
            Address toAddress = null;

            toUri = addressFactory.createURI(to);
            if (toUri.isSipURI() == false) {
                _logger.error("Invalid sip uri: " + mySipAddress);
                throw new SipException("Invalid sip uri: " + mySipAddress);
            }
            toAddress = addressFactory.createAddress(toUri);
            ToHeader toHeader = headerFactory.createToHeader(toAddress, null);

            // create Request URI
            String peerHostPort = peerHost + ":" + peerPort;
            SipURI requestURI = addressFactory.createSipURI(((SipURI) toUri).getUser(), peerHostPort);

            // Create ViaHeaders

            ArrayList viaHeaders = new ArrayList();
            ViaHeader viaHeader = headerFactory.createViaHeader(hostIpAddress, sipProvider.getListeningPoint(
                    transport).getPort(), transport, null);

            // add via headers
            viaHeaders.add(viaHeader);

            // Create a new CallId header
            CallIdHeader callIdHeader = sipProvider.getNewCallId();

            // Create a new Cseq header
            CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(1L, Request.INVITE);

            // Create a new MaxForwardsHeader
            MaxForwardsHeader maxForwards = headerFactory.createMaxForwardsHeader(70);

            // Create the request.
            Request request = messageFactory.createRequest(requestURI, Request.INVITE, callIdHeader,
                    cSeqHeader, fromHeader, toHeader, viaHeaders, maxForwards);

            // Add the contact address.
            ContactHeader contactHeader = headerFactory.createContactHeader(contactAddress);
            request.addHeader(contactHeader);

            // create and add the Route Header
            // Dont use the Outbound Proxy. Use Lr instead.
            SipURI sipuri = addressFactory.createSipURI(null, hostIpAddress);
            sipuri.setPort(peerPort);
            sipuri.setLrParam();
            sipuri.setTransportParam(transport);
            RouteHeader routeHeader = headerFactory.createRouteHeader(addressFactory.createAddress(sipuri));
            request.setHeader(routeHeader);

            // Create ContentTypeHeader
            ContentTypeHeader contentTypeHeader = headerFactory.createContentTypeHeader("application", "sdp");

            // add the message body (sdp)
            request.setContent(message.getSessionDescription().toString(), contentTypeHeader);

            // Header callInfoHeader = headerFactory.createHeader(
            // "Call-Info", "<http://www.antd.nist.gov>");
            // request.addHeader(callInfoHeader);

            // Create the client transaction.
            ClientTransaction ctx = sipProvider.getNewClientTransaction(request);

            // send the request out.
            ctx.sendRequest();

            Dialog dialog = ctx.getDialog();
            session = SipSession.createSipSession(this, ctx, null);
            SipSession.addPendingSession(session);

        } catch (TransactionUnavailableException e) {
            _logger.debug(e, e);
            throw e;
        } catch (SipException e) {
            _logger.debug(e, e);
            throw e;
        } catch (ParseException e) {
            _logger.debug(e, e);
            throw new SipException("Could not send invite due to a parse error in SIP stack.", e);
        } catch (InvalidArgumentException e) {
            _logger.debug(e, e);
            throw new SipException("Could not send invite due to invalid argument in SIP stack.", e);
        }

        return session;
    }
    
    public void sendBye(SipSession session)  throws SipException {
        Dialog d = session.getSipDialog();
        Request byeRequest;
        byeRequest = d.createRequest(Request.BYE);
        ClientTransaction ct = sipProvider.getNewClientTransaction(byeRequest);
        d.sendRequest(ct);

    }

    public String getGUID() {
        // counter++;
        // return guidPrefix+counter;
        int r = random.nextInt();
        r = (r < 0) ? 0 - r : r; // generate a positive number
        return Integer.toString(r);
    }

    /**
     * @return the sipAddress
     */
    public String getSipAddress() {
        return sipAddress;
    }

    /**
     * @param sipAddress
     *            the sipAddress to set
     */
    public void setSipAddress(String sipAddress) {
        this.sipAddress = sipAddress;
    }

    /**
     * @return the sessionListener
     */
    public SessionListener getSessionListener() {
        return sessionListener;
    }

    /**
     * @param sessionListener
     *            the sessionListener to set
     */
    public void setSessionListener(SessionListener sessionListener) {
        this.sessionListener = sessionListener;
    }

    /**
     * @return the myAddress
     */
    public Address getMyAddress() {
        return myAddress;
    }

    /**
     * @param myAddress
     *            the myAddress to set
     */
    public void setMyAddress(Address myAddress) {
        this.myAddress = myAddress;
    }

}
