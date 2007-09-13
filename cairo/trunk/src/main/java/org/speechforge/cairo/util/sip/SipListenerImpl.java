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

import java.rmi.RemoteException;
import java.text.ParseException;

import javax.sdp.SdpException;
import javax.sdp.SdpFactory;
import javax.sdp.SdpParseException;
import javax.sdp.SessionDescription;
import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.DialogState;
import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipException;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.address.Address;
import javax.sip.header.CSeqHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.ToHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.apache.log4j.Logger;
import org.speechforge.cairo.exception.ResourceUnavailableException;

/**
 * Implements the JAINSIP SipListener interface. Receives the low level sip
 * events via JAIN SIP.
 * 
 * @author Spencer Lord {@literal <}<a href="mailto:salord@users.sourceforge.net">salord@users.sourceforge.net</a>{@literal >}
 */
public class SipListenerImpl implements SipListener {

    private static Logger _logger = Logger.getLogger(SipListenerImpl.class);

    private SipAgent sipClient;

    public SipListenerImpl(SipAgent sipClient) {
        this.sipClient = sipClient;
    }

    public void processDialogTerminated(DialogTerminatedEvent arg0) {
        _logger.info("Got a dialog terminated event");
    }

    public void processIOException(IOExceptionEvent arg0) {
        _logger.info("Got an IO Exception");
    }

    public void processRequest(RequestEvent requestEvent) {

        Request request = requestEvent.getRequest();
        ServerTransaction stx = requestEvent.getServerTransaction();
        _logger.debug("Request " + request.getMethod() + " received at "
                + sipClient.getSipStack().getStackName() + " with server transaction id " + stx);
        /*
         * 
         * TODO: Check if the request is really addressed to me. check To header
         * ot route or contact header? ToHeader to = (ToHeader)
         * request.getHeader(ToHeader.NAME); if
         * (sipClient.getMyAddress().getURI().toString().equals(
         * to.getAddress().getURI().getScheme())) {
         */

        if (request.getMethod().equals(Request.INVITE)) {
            processInvite(requestEvent);
        } else if (request.getMethod().equals(Request.ACK)) {
            processAck(requestEvent);
        } else if (request.getMethod().equals(Request.BYE)) {
            processBye(requestEvent);
        } else if (request.getMethod().equals(Request.CANCEL)) {
            processCancel(requestEvent);
        } else {
            // TODO: this snippet is taken from the shootist example. Shootme
            // only has teh first line
            // I dont really undersatnd why it is sending an accepted response
            // and thaen a REFER request
            // and why the shootist example just sends the response. I would
            // think it should be symetrical.
            try {
                _logger.info("Got an unhandled SIP request Method = " + request.getMethod());
                stx.sendResponse(sipClient.getMessageFactory().createResponse(202, request));
                // send one back
                SipProvider prov = (SipProvider) requestEvent.getSource();
                Request refer = requestEvent.getDialog().createRequest("REFER");
                requestEvent.getDialog().sendRequest(prov.getNewClientTransaction(refer));
            } catch (SipException e) {
                _logger.error(e, e);
            } catch (InvalidArgumentException e) {
                _logger.error(e, e);
            } catch (ParseException e) {
                _logger.error(e, e);
            }
        }
    }

    public void processResponse(ResponseEvent responseEvent) {

        Dialog dialog = null;
        SipSession session = null;

        Response response = (Response) responseEvent.getResponse();
        ClientTransaction ctx = responseEvent.getClientTransaction();
        CSeqHeader cseq = (CSeqHeader) response.getHeader(CSeqHeader.NAME);

        _logger.debug("Sip Response received : Status Code = " + response.getStatusCode() + " cseq= " + cseq
                + "request method:" + cseq.getMethod());

        if (ctx != null) {
            dialog = ctx.getDialog();
        } else {
            _logger.info("Stray response -- dropping ");
            return;
        }

        try {
            if (response.getStatusCode() == Response.OK) {
                if (cseq.getMethod().equals(Request.INVITE)) {
                    // Got an INVITE OK
                    Request ackRequest = dialog.createRequest(Request.ACK);
                    dialog.sendAck(ackRequest);

                    // put the dialog into the session and remove from pending
                    // map and place into active session map
                    session = SipSession.getSessionFromPending(ctx.toString());

                    if (session != null) {
                        session.setSipDialog(dialog);
                        SipSession.moveFromPending(session);

                        byte[] contentBytes = response.getRawContent();
                        SdpFactory sdpFactory = SdpFactory.getInstance();

                        if (contentBytes == null) {
                            _logger.info("No content in the response.");
                        }
                        String contentString = new String(contentBytes);
                        SessionDescription sd = null;
                        try {
                            sd = sdpFactory.createSessionDescription(contentString);
                        } catch (SdpParseException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }

                        SdpMessage sdpMessage = SdpMessage.createSdpSessionMessage(sd);
                        SdpMessage sdpResponse = sipClient.getSessionListener().processInviteResponse(
                                sdpMessage, session);
                    } else {
                        // TODO: handle error condition where the session was
                        // not in the pending map
                        _logger.info("SIP Invite Response received but the session was not in pending map. "
                                + response.toString());
                    }

                } else if (cseq.getMethod().equals(Request.CANCEL)) {
                    // TODO: handle cancel processing properly
                    if (dialog.getState() == DialogState.CONFIRMED) {
                        // oops cancel went in too late. Need to hang up the
                        // dialog.
                        // System.out.println("Sending BYE -- cancel went in too
                        // late !!");
                        Request byeRequest = dialog.createRequest(Request.BYE);
                        ClientTransaction ct = sipClient.getSipProvider().getNewClientTransaction(byeRequest);
                        dialog.sendRequest(ct);
                    }
                } else if (cseq.getMethod().equals(Request.BYE)) {
                }
            } else if (response.getStatusCode() == Response.RINGING) {

            } else if (response.getStatusCode() == Response.REQUEST_TERMINATED) {

            }
        } catch (SipException e) {
            // TODO: Handle case where there is an exception handling the invite
            // response. I think We still need to respond with an ACK (or NACK)
            _logger.error(e, e);
        }
    }

    public void processTimeout(TimeoutEvent arg0) {
        _logger.debug("Transaction Time out");
    }

    public void processTransactionTerminated(TransactionTerminatedEvent arg0) {
        _logger.debug("Got a transaction terminated event");
    }

    /**
     * Process the ACK request.
     */
    public void processAck(RequestEvent requestEvent) {
        // _logger.info("Got a ACK event");
        // ServerTransaction stx = requestEvent.getServerTransaction();
        // Dialog dialog = stx.getDialog();
        // SipSession session = SipSession.getSession(dialog.getDialogId());
    }

    /**
     * Process the invite request.
     */
    public void processInvite(RequestEvent requestEvent) {
        SipProvider sipProvider = (SipProvider) requestEvent.getSource();
        Request request = requestEvent.getRequest();
        Response okResponse = null;
        ServerTransaction stx = null;
        SipSession session = null;
        boolean reinvite = false;
        try {

            stx = requestEvent.getServerTransaction();
            if (stx == null) {
                stx = sipProvider.getNewServerTransaction(request);
                session = SipSession.createSipSession(sipClient, null, null);

            } else {
                Dialog dialog = stx.getDialog();
                if (dialog != null) {
                    // TODO: handle a re-invite. This must be a re-invite if
                    // there already is a dialog.
                    reinvite = true;
                    session = SipSession.getSession(dialog.getDialogId());
                    _logger.info("Recieved a re-invite request.  Not supported yet");
                }
            }
            Address address = sipClient.getAddressFactory().createAddress(
                    "<sip:" + sipClient.getHost() + ":" + sipClient.getPort() + ">");
            ContactHeader contactHeader = sipClient.getHeaderFactory().createContactHeader(address);

            byte[] contentBytes = request.getRawContent();
            SdpFactory sdpFactory = SdpFactory.getInstance();

            boolean noOffer = false;
            if (contentBytes == null) {
                // TODO: How to deal with the absense of an offer in the invite
                // the sepc says that the UAS should send an offer in the 2xx
                // response and
                // expect a response to the offer in the ACK (Does it make sense
                // here what should the server offer
                // two mrcp channels and a rtp channel?
                noOffer = true;
                _logger.info("No offer in the invite request.  Should provide offer in response but not supported yet.");
            } else {
                String contentString = new String(contentBytes);
                SessionDescription sd = sdpFactory.createSessionDescription(contentString);
                SdpMessage sdpMessage = SdpMessage.createSdpSessionMessage(sd);

                //valudate the sdp message (throw sdpException if the message is invalid)
                SdpMessageValidator.validate(sdpMessage);

                //process the invitaion (the resource manager processInviteRequest method)
                SdpMessage sdpResponse = sipClient.getSessionListener().processInviteRequest(sdpMessage,
                        session);

                // send the ok (assuming that the offer is accepted with the response in the sdpMessaage)
                //TODO what if the offer is not accepted?  Do all non-ok response come thru the exception path?
                okResponse = sipClient.getMessageFactory().createResponse(Response.OK, request);

                // Create a application/sdp ContentTypeHeader
                ContentTypeHeader contentTypeHeader = sipClient.getHeaderFactory().createContentTypeHeader(
                        "application", "sdp");

                // add the sdp response to the message
                okResponse.setContent(sdpResponse.getSessionDescription().toString(), contentTypeHeader);

                ToHeader toHeader = (ToHeader) okResponse.getHeader(ToHeader.NAME);
                toHeader.setTag(sipClient.getGUID());
                okResponse.addHeader(contactHeader);

            }
        } catch (SipException e) {
            OfferRejected();
            _logger.info("Could not process invite." + e, e);
        } catch (ParseException e) {
            OfferRejected();
            _logger.info("Could not process invite." + e, e);
        } catch (ResourceUnavailableException e) {
            OfferRejected();
            _logger.info("Could not process invite." + e, e);
        } catch (RemoteException e) {
            OfferRejected();
            _logger.info("Could not process invite." + e, e);
        } catch (SdpException e) {
            OfferRejected();
            _logger.info("Could not process invite." + e, e);
        }

        // Now if there were no exceptions, we were able to process the invite
        // request and we have a valid reponse to send back
        // if there is an exception here, not much that can be done.
        try {
            stx.sendResponse(okResponse);
            ;
        } catch (SipException e) {
            _logger.error(e, e);
        } catch (InvalidArgumentException e) {
            _logger.error(e, e);
        }

        // Now that the dialog was created, set the dialog in the session
        session.setSipDialog(stx.getDialog());
        SipSession.addSession(session);
    }

    private void OfferRejected() {
        // TODO: processing of a rejected offer or rejected call
        // 1) If the offer is rejected the spec says INVITE SHOULD return a 488
        // (Not Acceptable Here) response. Such a response
        // SHOULD include a Warning header field value explaining why the offer
        // was rejected.
        // 2) If the callee is currently not willing or able to take additional
        // calls at this end system. A 486 (Busy Here)
        // SHOULD be returned in such a scenario.
        _logger.info("Should send a 488 or 486 response to the invite.  Not implemented yet");
    }

    public void processCancel(RequestEvent requestEvent) {
        // SipProvider sipProvider = (SipProvider) requestEvent.getSource();
        Request request = requestEvent.getRequest();
        ServerTransaction stx = requestEvent.getServerTransaction();
        // Dialog dialog = stx.getDialog();
        try {
            Response response = sipClient.getMessageFactory().createResponse(200, request);
            stx.sendResponse(response);

            // Not sure if this is really required. Do I need to save the invite
            // requests
            // that was sent earlier nd is now being cacnelled? Cancellation is
            // not needed
            // for now in any case...
            // if (dialog.getState() != DialogState.CONFIRMED) {
            // response = sipClient.getMessageFactory().createResponse(
            // Response.REQUEST_TERMINATED, inviteRequest);
            // stx.sendResponse(response);
            // }

        } catch (Exception e) {
            _logger.error(e, e);
        }
    }

    /**
     * Process the bye request.
     */
    public void processBye(RequestEvent requestEvent) {
        // SipProvider sipProvider = (SipProvider) requestEvent.getSource();
        Request request = requestEvent.getRequest();
        ServerTransaction stx = requestEvent.getServerTransaction();
        Dialog dialog = requestEvent.getDialog();
        SipSession session = SipSession.getSession(dialog.getDialogId());

        // TODO: check for any pending requests. The spec says that the
        // "UAS MUST still respond to any pending requests received for that
        // dialog. It is RECOMMENDED that a 487 (Request Terminated) response
        // be generated to those pending requests."
        try {
            // TODO: anything else needed to cleanup session. close mrcp
            // channels, put back resources etc. (part of a bigger project)
            SipSession.removeSession(session);
            Response response = sipClient.getMessageFactory().createResponse(200, request);
            stx.sendResponse(response);
        } catch (SipException e) {
            _logger.error(e, e);
        } catch (ParseException e) {
            _logger.error(e, e);
        } catch (InvalidArgumentException e) {
            _logger.error(e, e);
        }
    }
}
