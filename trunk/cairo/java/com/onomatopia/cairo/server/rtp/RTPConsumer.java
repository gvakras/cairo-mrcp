/*
 * Cairo - Open source framework for control of speech media resources.
 *
 * Copyright (C) 2005 Onomatopia, Inc. - http://www.onomatopia.com
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
package com.onomatopia.cairo.server.rtp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.media.protocol.DataSource;
import javax.media.protocol.PushBufferDataSource;
import javax.media.rtp.InvalidSessionAddressException;
import javax.media.rtp.Participant;
import javax.media.rtp.RTPControl;
import javax.media.rtp.RTPManager;
import javax.media.rtp.ReceiveStream;
import javax.media.rtp.ReceiveStreamListener;
import javax.media.rtp.SessionAddress;
import javax.media.rtp.SessionListener;
import javax.media.rtp.event.ByeEvent;
import javax.media.rtp.event.InactiveReceiveStreamEvent;
import javax.media.rtp.event.NewParticipantEvent;
import javax.media.rtp.event.NewReceiveStreamEvent;
import javax.media.rtp.event.ReceiveStreamEvent;
import javax.media.rtp.event.RemotePayloadChangeEvent;
import javax.media.rtp.event.SessionEvent;
import javax.media.rtp.event.StreamMappedEvent;
import javax.media.rtp.rtcp.SourceDescription;

// TODO: convert to abstract base class

/**
 * TODOC
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 *
 */
public abstract class RTPConsumer implements SessionListener, ReceiveStreamListener {

    protected RTPManager _rtpManager;
    private SessionAddress _localAddress;
    private SessionAddress _targetAddress;

    public RTPConsumer(int port) throws IOException {
        _localAddress = new SessionAddress(InetAddress.getLocalHost(), port);
        _targetAddress = _localAddress;
        init();
    }

    public RTPConsumer(int localPort, InetAddress remoteAddress, int remotePort) throws IOException {
        _localAddress = new SessionAddress(InetAddress.getLocalHost(), localPort);
        _targetAddress = new SessionAddress(remoteAddress, remotePort);
        init();
    }
    
    private void init() throws IOException {

        _rtpManager = RTPManager.newInstance();
        System.out.println("RTPManager class: " + _rtpManager.getClass().getName());
        _rtpManager.addSessionListener(this);
        _rtpManager.addReceiveStreamListener(this);

        try {
            _rtpManager.initialize(_localAddress);
            _rtpManager.addTarget(_targetAddress);
        } catch (InvalidSessionAddressException e) {
            throw (IOException) new IOException(e.getMessage()).initCause(e);
        }
    }

    /**
     * TODOC
     */
    public synchronized void shutdown() {
        // close RTP streams
        if (_rtpManager != null) {
            _rtpManager.removeTargets("RTP receiver shutting down.");
            _rtpManager.dispose();
            _rtpManager = null;
        }

    }

    /* (non-Javadoc)
     * @see javax.media.rtp.SessionListener#update(javax.media.rtp.event.SessionEvent)
     */
    public synchronized void update(SessionEvent event) {
        System.out.println("SessionEvent received: " + event);
        if (event instanceof NewParticipantEvent) {
            Participant p = ((NewParticipantEvent) event).getParticipant();
            System.out.println("  - A new participant has just joined: " + p.getCNAME());
        }
    }

    /* (non-Javadoc)
     * @see javax.media.rtp.ReceiveStreamListener#update(javax.media.rtp.event.ReceiveStreamEvent)
     */
    public synchronized void update(ReceiveStreamEvent event) {
        System.out.println("ReceiveStreamEvent received: " + event);

//        if (event instanceof RemotePayloadChangeEvent) {
//            System.err.println("  - Received an RTP PayloadChangeEvent.");
//            System.err.println("Sorry, cannot handle payload change.");
//            //System.exit(0);
//            return;
//        }

        ReceiveStream stream = event.getReceiveStream();


        if (event instanceof NewReceiveStreamEvent) {
//            if (stream == null) {
//                System.out.println("NewReceiveStreamEvent: receive stream is null!");
//            } else {
                DataSource dataSource = stream.getDataSource();
//                if (dataSource == null) {
//                    System.out.println("NewReceiveStreamEvent: data source is null!");
//                } else if (!(dataSource instanceof PushBufferDataSource)) {
//                    System.out.println("NewReceiveStreamEvent: data source is not PushBufferDataSource!");
//                } else {
//                    // Find out the formats.
//                    RTPControl control = (RTPControl) dataSource.getControl("javax.media.rtp.RTPControl");
//                    if (control != null) {
//                        System.out.println("  - Recevied new RTP stream: " + control.getFormat());
//                    } else {
//                        System.out.println("  - Recevied new RTP stream: RTPControl is null!");
//                    }
                    this.streamReceived(stream, (PushBufferDataSource) dataSource);
//                }
//
//            }
        } else if (event instanceof StreamMappedEvent) {
            Participant participant = event.getParticipant();
            if (participant != null) {
                for (Iterator it = participant.getSourceDescription().iterator(); it.hasNext(); ) {
                    SourceDescription sd = (SourceDescription) it.next();
                    System.out.println("Source description: " + toString(sd));
                }
            }
            if (stream == null) {
                System.out.println("StreamMappedEvent: receive stream is null!");
            } else if (participant == null) {
                System.out.println("StreamMappedEvent: participant is null!");
            } else {
                this.streamMapped(stream, participant);
            }
        } else if (event instanceof InactiveReceiveStreamEvent || event instanceof ByeEvent) {
            if (stream != null) {
                this.streamInactive(stream, (event instanceof ByeEvent));
            }

        }
    }

    public abstract void streamReceived(ReceiveStream stream, PushBufferDataSource dataSource);

    public abstract void streamMapped(ReceiveStream stream, Participant participant);

    public abstract void streamInactive(ReceiveStream stream, boolean byeEvent);

    private static String toString(SourceDescription sd) {
        StringBuffer sb = new StringBuffer();
        switch (sd.getType()) {
        case SourceDescription.SOURCE_DESC_CNAME:
            sb.append("SOURCE_DESC_CNAME");
            break;
        
        case SourceDescription.SOURCE_DESC_NAME:
            sb.append("SOURCE_DESC_NAME");
            break;
        
        case SourceDescription.SOURCE_DESC_EMAIL:
            sb.append("SOURCE_DESC_EMAIL");
            break;
        
        case SourceDescription.SOURCE_DESC_PHONE:
            sb.append("SOURCE_DESC_PHONE");
            break;
        
        case SourceDescription.SOURCE_DESC_LOC:
            sb.append("SOURCE_DESC_LOC");
            break;
        
        case SourceDescription.SOURCE_DESC_TOOL:
            sb.append("SOURCE_DESC_TOOL");
            break;
        
        case SourceDescription.SOURCE_DESC_NOTE:
            sb.append("SOURCE_DESC_NOTE");
            break;
        
        case SourceDescription.SOURCE_DESC_PRIV:
            sb.append("SOURCE_DESC_PRIV");
            break;

        default:
            sb.append("SOURCE_DESC_???");
            break;

        }
        sb.append('=').append(sd.getDescription());
        return sb.toString();
    }

}
