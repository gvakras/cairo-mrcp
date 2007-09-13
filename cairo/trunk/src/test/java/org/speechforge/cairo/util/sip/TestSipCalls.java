package org.speechforge.cairo.util.sip;

import gov.nist.javax.sip.header.SIPHeaderNames;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Properties;
import javax.sdp.SdpException;
import javax.sip.ObjectInUseException;
import javax.sip.SipException;
import org.cafesip.sipunit.SipCall;
import org.cafesip.sipunit.SipPhone;
import org.cafesip.sipunit.SipResponse;
import org.cafesip.sipunit.SipStack;
import org.cafesip.sipunit.SipTestCase;
import org.speechforge.cairo.util.sip.SipAgent;
import org.speechforge.cairo.util.sip.SdpMessage;
import org.speechforge.cairo.util.sip.SessionListener;
import org.speechforge.cairo.util.sip.SipSession;

public class TestSipCalls extends SipTestCase implements SessionListener {

    String host;

    private Properties properties = new Properties();

    public void setUp() throws Exception {

        host = null;
        try {
            host = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            host = "localhost";
        }

        properties.setProperty("javax.sip.IP_ADDRESS", host);
        properties.setProperty("javax.sip.STACK_NAME", "testAgent");
        properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "0");
        properties.setProperty("gov.nist.javax.sip.DEBUG_LOG", "testAgent_debug.txt");
        properties.setProperty("gov.nist.javax.sip.SERVER_LOG", "testAgent_log.txt");
        properties.setProperty("gov.nist.javax.sip.READ_TIMEOUT", "1000");
        properties.setProperty("gov.nist.javax.sip.CACHE_SERVER_CONNECTIONS", "false");

        properties.setProperty("sipunit.trace", "true");
        properties.setProperty("sipunit.test.port", "5061");
        properties.setProperty("sipunit.test.protocol", "udp");

    }

    public void tearDown() throws Exception {

    }

    public void testServerResponseUDP() throws SipException {

        SipStack sipStack = null;
        SipPhone ua = null;
        try {
            properties.setProperty("sipunit.test.port", "5061");
            sipStack = new SipStack(SipStack.PROTOCOL_UDP, 5061, properties);

            ua = sipStack.createSipPhone(host, SipStack.PROTOCOL_UDP, 5061, "sip:slord@speechforge.org");

        } catch (Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }


        SipAgent cairoUA = new SipAgent(this, "sip:cairo@speechforge.org", "name", 5062, "udp");

        // use sipunit to make a call
        SipCall call = ua.makeCall("sip:cairo@speechforge.org", host + ":5062/UDP",
                SdpTestMessages.inviteRequest3, "application", "sdp", null, null);
        assertLastOperationSuccess(ua.format(), ua);

        // System.out.println( SdpTestMessages.inviteRequest3);

        // poll for an OK response (200)
        boolean notDone = true;
        while (notDone) {

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            int rcode = call.getReturnCode();
            // System.out.println("The return code is: " +rcode);
            if (rcode == 200) { // ok code
                notDone = false;
            }
        }

        // ------
        assertTrue("Wrong number of responses received", call.getAllReceivedResponses().size() == 1);

        // verify RINGING was received
        // assertResponseReceived("Should have gotten TRYING response",
        // SipResponse.TRYING, call);

        // assertResponseReceived("Should have gotten RINGING response",
        // SipResponse.RINGING, call);

        // verify OK was received
        assertResponseReceived(SipResponse.OK, call);

        // check negative
        assertResponseNotReceived("Unexpected response", SipResponse.NOT_FOUND, call);

        assertResponseNotReceived(SipResponse.ADDRESS_INCOMPLETE, call);

        // verify getLastReceivedResponse() method
        assertEquals("Last response received wasn't answer", SipResponse.OK, call.getLastReceivedResponse()
                .getStatusCode());

        call.sendInviteOkAck();
        assertLastOperationSuccess("Failure sending ACK - " + call.format(), call);
        call.listenForDisconnect();

        // TODO: Have server disconnect
        // cairoUA.disconnect(call);
        // call.waitForDisconnect(3000);
        // assertLastOperationSuccess("Wait disc - " + call.format(), call);
        // call.respondToDisconnect();

        // check all of the responses along the way (should be two)
        ArrayList responses = call.getAllReceivedResponses();
        // System.out.println("Got "+responses.size()+ " responses");
        for (Object o : responses) {
            SipResponse response = (SipResponse) o;
            // System.out.println("-----------------------------------------------------------");
            // System.out.println("Status code & reason: " + response.getStatusCode() + " "
            // +response.getReasonPhrase());
            // Message message =response.getMessage();

            assertHeaderPresent(SIPHeaderNames.FROM + " Header not present", response, SIPHeaderNames.FROM);
            assertHeaderPresent(SIPHeaderNames.TO + " Header not present", response, SIPHeaderNames.TO);
            assertHeaderPresent(SIPHeaderNames.VIA + " Header not present", response, SIPHeaderNames.VIA);
            assertHeaderPresent(SIPHeaderNames.CSEQ + " Header not present", response, SIPHeaderNames.CSEQ);
            assertHeaderPresent(SIPHeaderNames.CALL_ID + " Header not present", response,
                    SIPHeaderNames.CALL_ID);

            // System.out.println(message);
        }

        // check for contact header, content header adn body on teh invike ok resposne only
        SipResponse response = call.getLastReceivedResponse();
        assertHeaderPresent(SIPHeaderNames.CONTENT_TYPE + " Header not present", response,
                SIPHeaderNames.CONTENT_TYPE);
        assertHeaderPresent(SIPHeaderNames.CONTACT + " Header not present", response, SIPHeaderNames.CONTACT);
        assertBodyPresent("Body not present in response", response);

        ua.dispose();
        sipStack.dispose();
        cairoUA.dispose();

    }

    public void testSendInviteUDP() throws SipException, SdpException {

        int peerPort = 5070;
        String peerAddress = host;
        String receiverSipAddress = "sip:receiver@speechforge.org";
        String senderSipAddress = "sip:sender@speechforge.org";


        // setup the server with this as the sessionlistener
        SipAgent receiver = new SipAgent(this, receiverSipAddress, "serverStack2", 5070, "UDP");
        SipAgent sender = new SipAgent(this, senderSipAddress, "serverStack3", 5071, "UDP");

        SdpMessage message = SdpMessage
                .createNewSdpSessionMessage(senderSipAddress, host, "The session Name");
        sender.sendInviteWithoutProxy(receiverSipAddress, message, peerAddress, peerPort);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {
            receiver.dispose();
            sender.dispose();
        } catch (ObjectInUseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    /*
     * public void testSendInviteUDP2() {
     * 
     * int testReceiverPort = 5062; String testReceiverAddress = "127.0.0.1";
     * 
     * try { properties.setProperty("sipunit.test.port", String.valueOf((testReceiverPort))); String to =
     * "sip:cairo@speechforge.org"; SipStack sipStack = new SipStack(SipStack.PROTOCOL_UDP, testReceiverPort,
     * properties); SipPhone testReceiver = sipStack.createSipPhone( null, SipStack.PROTOCOL_UDP,
     * testReceiverPort, to);
     * 
     * SIPFactory factory = SIPFactory.newInstance(); SIPAgent cairoUA =
     * factory.createAgent(this,"sip:cairo@speechforge.org","name",5062,"udp");
     * 
     * SdpMessage message = SdpMessage.createNewSdpSessionMessage("sip:slord@127.0.0.1", host, "The session
     * Name"); cairoUA.sendInviteWithoutProxy(to, message, testReceiverAddress, testReceiverPort);
     * 
     * 
     * testReceiver.listenRequestMessage(); RequestEvent inc_req = testReceiver.waitRequest(30000);
     * assertNotNull(testReceiver.format(), inc_req); // call received
     * 
     * Response response = null;
     * 
     * response = testReceiver.getParent().getMessageFactory() .createResponse(Response.TRYING,
     * inc_req.getRequest());
     * 
     * SipTransaction transb = testReceiver.sendReply(inc_req, response); assertNotNull(testReceiver.format(),
     * transb); javax.sip.address.URI callee_contact = testReceiver.getParent().getAddressFactory().createURI(
     * "sip:slord@" + testReceiverAddress + ':' + testReceiverPort); Address contact =
     * testReceiver.getParent().getAddressFactory().createAddress( callee_contact);
     * 
     * String to_tag = testReceiver.generateNewTag();
     * 
     * testReceiver.sendReply(transb, Response.RINGING, null, to_tag, contact, -1);
     * assertLastOperationSuccess(testReceiver.format(), testReceiver); // ringing response sent
     * 
     * response = testReceiver.getParent().getMessageFactory().createResponse( Response.OK,
     * inc_req.getRequest()); response.addHeader(testReceiver.getParent().getHeaderFactory()
     * .createContactHeader(contact));
     * 
     * testReceiver.sendReply(transb, response); assertLastOperationSuccess(testReceiver.format(),
     * testReceiver); // answer response sent
     * 
     * 
     * testReceiver.dispose(); sipStack.dispose(); cairoUA.dispose();
     *  } catch (InvalidArgumentException e) { // TODO Auto-generated catch block e.printStackTrace(); } catch
     * (ParseException e) { // TODO Auto-generated catch block e.printStackTrace(); } catch (Exception e) { //
     * TODO Auto-generated catch block e.printStackTrace(); }
     *  }
     */

    public SdpMessage processByeRequest(SdpMessage request, SipSession session) {
        // TODO Auto-generated method stub
        return null;
    }

    public SdpMessage processInviteRequest(SdpMessage request, SipSession session) {
        // System.out.println("Got a invite Request");
        // System.out.println(request.getSessionDescription().toString());
        return SdpMessage.createSdpSessionMessage(request.getSessionDescription());

    }

    public SdpMessage processInviteResponse(SdpMessage response, SipSession session) {
        // System.out.println("Got a invite Response");
        // System.out.println(response.getSessionDescription().toString());
        return null;

    }

}
