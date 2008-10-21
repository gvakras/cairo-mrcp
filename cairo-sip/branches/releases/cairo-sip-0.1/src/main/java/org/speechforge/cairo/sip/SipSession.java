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
package org.speechforge.cairo.sip;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.RequestEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipException;
import javax.sip.message.Request;

import org.apache.log4j.Logger;
import org.mrcp4j.client.MrcpChannel;

/**
 * Represents a SIP session.
 * 
 * @author Spencer Lord {@literal <}<a href="mailto:salord@users.sourceforge.net">salord@users.sourceforge.net</a>{@literal >}
 */
public class SipSession {

    private static Logger _logger = Logger.getLogger(SipSession.class);

    //common to server and client tx
    private SipAgent agent;
    
    //server side transaction 
    private Dialog sipDialog;
    private RequestEvent request;
    private ServerTransaction stx;
    private List<SipResource> resources;
    private SipSession forward;
    
    private String channelName;
    private String applicationName;
    
    
    //client side transaction
    private ClientTransaction ctx;
    private MrcpChannel ttsChannel;
    private MrcpChannel recogChannel;

    //private  SdpMessage lastRequest;
    //private  SdpMessage lastResponse;
    
    private static Map<String, SipSession> sessions = new Hashtable<String, SipSession>();
    private static Map<String, SipSession> pendingSessions = new Hashtable<String, SipSession>();

    public SipSession() {
        resources = new ArrayList<SipResource>();
    } 
    
    /**
     * @return the ctx
     */
    public ClientTransaction getCtx() {
        return ctx;
    }

    /**
     * @param ctx
     *            the ctx to set
     */
    public void setCtx(ClientTransaction ctx) {
        this.ctx = ctx;
    }

    /**
     * @return the sipDialog
     */
    public Dialog getSipDialog() {
        return sipDialog;
    }

    /**
     * @param sipDialog
     *            the sipDialog to set
     */
    public void setSipDialog(Dialog sipDialog) {
        this.sipDialog = sipDialog;
    }

    /**
     * @return the agent
     */
    public SipAgent getAgent() {
        return agent;
    }

    /**
     * @param agent
     *            the agent to set
     */
    public void setAgent(SipAgent agent) {
        this.agent = agent;
    }

    /**
     * @return the resources
     */
    public List<SipResource> getResources() {
        return resources;
    }

    /**
     * @param resources the resources to set
     */
    public void setResources(List<SipResource> resources) {
        this.resources = resources;
    }
    
    public String getId() {
        return sipDialog.getDialogId();
    }
    
    /**
     * @return the forward
     */
    public SipSession getForward() {
        return forward;
    }

    /**
     * @param forward the forward to set
     */
    public void setForward(SipSession forward) {
        this.forward = forward;
    }
    
    
    public void bye() throws SipException {
        agent.sendBye(this);
    }

    public void reInvite() {
        // TODO: implement modifying the session via re-invite
    }

    public static SipSession createSipSession(SipAgent agent, ClientTransaction ctx, Dialog d, RequestEvent request, 
                                              ServerTransaction stx, String channelName, String applicationName) {
        SipSession s = new SipSession();
        s.agent = agent;
        s.ctx = ctx;
        s.sipDialog = d;
        s.request = request;
        s.stx = stx;
        s.channelName = channelName;
        s.applicationName = applicationName;
        return s;
    }

    public static synchronized void addPendingSession(SipSession session) {

        if (session.getCtx() != null) {
            pendingSessions.put(session.getCtx().toString(), session);
        } else {
            // TODO: invalid session
            _logger.info("Can not add to pending queue.  Invalid session.  No client side tx.");
        }
    }

    public static synchronized void moveFromPending(SipSession session) {
        if (session.getSipDialog() != null) {
            if (session.getCtx() != null) {
                String key = session.getCtx().toString();
                SipSession s = pendingSessions.get(key);
                pendingSessions.remove(key);
                sessions.put(session.getSipDialog().getDialogId(), s);
            } else {
                // TODO: invalid session
                _logger.info("Can not move from pending queue to established queue.  Invalid session.  No client side tx.");
            }
        } else {
            // TODO: invalid session
            _logger.info("Can not move from pending queue to established queue.  Invalid session.  No dialog.");
        }
    }

    public static synchronized void addSession(SipSession session) {
        if (session.getSipDialog() != null) {
            sessions.put(session.getSipDialog().getDialogId(), session);
        } else {
            // TODO: invalid session
            _logger.info("Can not add to session queue.  Invalid session.  No dialog.");
        }
    }

    public static synchronized void removeSession(SipSession session) {
        if (session.getSipDialog() != null) {
            sessions.remove(session.getSipDialog().getDialogId());
        } else {
            // TODO: invalid session
            _logger.info("Can not remove from session queue.  Invalid session.  No dialog.");
        }
    }

    public static synchronized void removeSessionFromPending(SipSession session) {
        if (session.getCtx() != null) {
            String key = session.getCtx().toString();
            pendingSessions.remove(key);
        } else {
            // TODO: invalid session
            _logger.info("Can not remove from pending queue.  Invalid session.  No client side tx.");
        }
    }

    public static synchronized SipSession getSession(String key) {
        return sessions.get(key);
    }

    public static synchronized SipSession getSessionFromPending(String key) {
        return pendingSessions.get(key);
    }

    /**
     * @return the recogChanel
     */
    public MrcpChannel getRecogChannel() {
        return recogChannel;
    }

    /**
     * @param recogChanel the recogChanel to set
     */
    public void setRecogChannel(MrcpChannel recogChanel) {
        this.recogChannel = recogChanel;
    }

    /**
     * @return the ttsChannel
     */
    public MrcpChannel getTtsChannel() {
        return ttsChannel;
    }

    /**
     * @param ttsChannel the ttsChannel to set
     */
    public void setTtsChannel(MrcpChannel ttsChannel) {
        this.ttsChannel = ttsChannel;
    }

    /**
     * @return the request
     */
    public RequestEvent getRequest() {
        return request;
    }

    /**
     * @param request the request to set
     */
    public void setRequest(RequestEvent request) {
        this.request = request;
    }

    /**
     * @return the stx
     */
    public ServerTransaction getStx() {
        return stx;
    }

    /**
     * @param stx the stx to set
     */
    public void setStx(ServerTransaction stx) {
        this.stx = stx;
    }

    /**
     * @return the channelName
     */
    public String getChannelName() {
        return channelName;
    }

    /**
     * @param channelName the channelName to set
     */
    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    /**
     * @return the applicationName
     */
    public String getApplicationName() {
        return applicationName;
    }

    /**
     * @param applicationName the applicationName to set
     */
    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

}
