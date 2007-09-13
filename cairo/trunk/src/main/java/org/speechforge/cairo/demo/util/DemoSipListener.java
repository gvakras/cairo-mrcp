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

import org.speechforge.cairo.util.sip.SdpMessage;
import org.speechforge.cairo.util.sip.SessionListener;
import org.speechforge.cairo.util.sip.SipSession;

public class DemoSipListener implements SessionListener {


	private SdpMessage response;


	private boolean noResponse = false;

	/**
	 * Session established.
	 * 
	 * @return true, if the seesion was established
	 */
	public boolean SessionEstablished() {
		return noResponse;
	}

	/**
	 * Gets the response.
	 * 
	 * @return the response
	 */
	public SdpMessage getResponse() {
		return response;
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

	public SdpMessage processInviteResponse(SdpMessage response,
			SipSession session) {
		this.response = response;
		noResponse = true;
		return null;
	}

}
