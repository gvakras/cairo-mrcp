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

import org.speechforge.cairo.server.config.CairoConfig;
import org.speechforge.cairo.server.config.TransmitterConfig;
import org.speechforge.cairo.server.tts.MrcpSpeechSynthChannel;
import org.speechforge.cairo.server.tts.PromptGeneratorFactory;
import org.speechforge.cairo.server.tts.RTPSpeechSynthChannel;
import org.speechforge.cairo.util.CairoUtil;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.pool.ObjectPool;
import org.apache.log4j.Logger;
import org.mrcp4j.MrcpResourceType;
import org.mrcp4j.server.MrcpServerSocket;

/**
 * TODOC
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 *
 */
public class TransmitterResource extends ResourceImpl {

    private static Logger _logger = Logger.getLogger(TransmitterResource.class);

    public static final Resource.Type RESOURCE_TYPE = Resource.Type.TRANSMITTER;

    private File _basePromptDir;
    private int _rtpBasePort;
    private MrcpServerSocket _mrcpServer;
    private ObjectPool _promptGeneratorPool;

    public TransmitterResource(TransmitterConfig config)
      throws IOException, RemoteException {
        super(RESOURCE_TYPE);
        _basePromptDir = config.getBasePromptDir();
        _rtpBasePort = config.getRtpBasePort();
        _mrcpServer = new MrcpServerSocket(config.getMrcpPort());
        _promptGeneratorPool = PromptGeneratorFactory.createObjectPool(15);
    }

    /* (non-Javadoc)
     * @see org.speechforge.cairo.server.resource.Resource#invite(org.speechforge.cairo.server.resource.ResourceMessage)
     */
    public ResourceMessage invite(ResourceMessage request) throws ResourceUnavailableException {
        _logger.debug("Resource received invite() request.");

        List<ResourceChannel> channels = new ArrayList<ResourceChannel>();

        for (ResourceChannel channel : request.getChannels()) {
            MrcpResourceType resourceType = channel.getResourceType();
            Type type = Resource.Type.fromMrcpType(resourceType);
            if (type.equals(RESOURCE_TYPE)) {
                channels.add(channel);
            }
        }

        if (channels.size() > 0) {
            try {
                ResourceMediaStream stream = request.getMediaStream();
                int localPort = _rtpBasePort+=2; // TODO: get from pool
                InetAddress remoteHost = InetAddress.getLocalHost();  // TODO: get from request
                int remotePort = stream.getPort();

                for (ResourceChannel channel : channels) {
                    String channelID = channel.getChannelID();

                    switch (channel.getResourceType()) {
                    case BASICSYNTH:
                    case SPEECHSYNTH:
                        MrcpSpeechSynthChannel mrcpChannel = new MrcpSpeechSynthChannel(
                            channelID,
                            new RTPSpeechSynthChannel(localPort, remoteHost, remotePort),
                            _basePromptDir,
                            _promptGeneratorPool
                        );
                        _mrcpServer.openChannel(channelID, mrcpChannel);
                        break;

                    default:
                        throw new ResourceUnavailableException("Unsupported resource type!");
                    }

                    channel.setPort(_mrcpServer.getPort());
                }

            } catch (ResourceUnavailableException e) {
                e.printStackTrace();
                throw e;
            } catch (Exception e) {
                e.printStackTrace();
                throw new ResourceUnavailableException(e);
            }
        }

        return request;
    }


    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            throw new Exception("Missing command line arguments, expected: <cairo-config-URL> <resource-name>");
        }

        URL configURL = CairoUtil.argToURL(args[0]);
        String resourceName = args[1];

        CairoConfig config = new CairoConfig(configURL);
        TransmitterConfig resourceConfig = config.getTransmitterConfig(resourceName);

        InetAddress host = InetAddress.getLocalHost();
        String url = "rmi://" + host.getHostName() + '/' + ResourceRegistry.NAME;
        System.out.println("looking up: " + url);
        ResourceRegistry resourceRegistry = (ResourceRegistry) Naming.lookup(url);

        TransmitterResource impl = new TransmitterResource(resourceConfig);

        System.out.println("binding transmitter resource...");
        resourceRegistry.register(impl, RESOURCE_TYPE);

        System.out.println("Resource bound and waiting...");
    }

}
