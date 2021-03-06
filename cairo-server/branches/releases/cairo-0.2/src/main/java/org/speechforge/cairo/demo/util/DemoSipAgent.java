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

package org.speechforge.cairo.demo.util;

import javax.sip.ObjectInUseException;
import javax.sip.SipException;
import javax.sip.TimeoutEvent;

import org.speechforge.cairo.util.sip.SdpMessage;
import org.speechforge.cairo.util.sip.SessionListener;
import org.speechforge.cairo.util.sip.SipAgent;
import org.speechforge.cairo.util.sip.SipSession;

public class DemoSipAgent implements SessionListener {

        private String _mySipAddress;
        private String _stackName;
        private int _port;
        private String _transport;
        private SipAgent _sipAgent;
        
        SdpMessage _response;
        SipSession _session;

        public DemoSipAgent(String mySipAddress, String stackName, int port, String transport) throws SipException {

            _mySipAddress = mySipAddress;
            _stackName = stackName;
            _port = port;
            _transport = transport;
        }
        
        public SdpMessage sendInviteWithoutProxy(String to, SdpMessage message, String peerAddress, int peerPort) throws SipException {

            // Construct a SIP agent to be used to send a SIP Invitation to the ciaro server
            //DemoSipListener listener = new DemoSipListener();

            _sipAgent =new SipAgent(this, _mySipAddress, _stackName, _port, _transport);

            // Send the sip invitation
            SipSession session = _sipAgent.sendInviteWithoutProxy(to, message, peerAddress, peerPort);

            synchronized (this) {
                try {
                    this.wait(1000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            return _response;
        }
        
        public void dispose() throws ObjectInUseException {
            _sipAgent.dispose();
        }
        

	public SdpMessage processByeRequest(SdpMessage request, SipSession session) {
		// TODO Auto-generated method stub
		return null;
	}


	public SdpMessage processInviteRequest(SdpMessage request,
			SipSession session) {
		// TODO Auto-generated method stub
		return null;
	}

	public synchronized SdpMessage processInviteResponse(SdpMessage response,SipSession session) {
		_response = response;
                _session = session;
                this.notify();
                return null;
	}

        public synchronized void processTimeout(TimeoutEvent event) {
            _response = null;
            this.notify();
    
        }

}
