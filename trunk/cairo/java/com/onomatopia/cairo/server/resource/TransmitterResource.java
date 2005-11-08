/**
 * TODOC
 * 
 */
package com.onomatopia.cairo.server.resource;


import com.onomatopia.cairo.server.config.CairoConfig;
import com.onomatopia.cairo.server.config.TransmitterConfig;
import com.onomatopia.cairo.server.tts.MrcpSpeechSynthChannel;
import com.onomatopia.cairo.server.tts.RTPSpeechSynthChannel;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import org.mrcp4j.MrcpResourceType;
import org.mrcp4j.server.MrcpServerSocket;

/**
 * TODOC
 * @author Niels
 *
 */
public class TransmitterResource extends ResourceImpl {

    public static final Resource.Type RESOURCE_TYPE = Resource.Type.TRANSMITTER;

    private MrcpServerSocket _mrcpServer;

    private TransmitterConfig _config;
    private File _basePromptDir;

    private int _rtpBasePort;

    public TransmitterResource(TransmitterConfig config)
      throws IOException, RemoteException {
        super(RESOURCE_TYPE);
        _basePromptDir = config.getBasePromptDir();
        _rtpBasePort = config.getRtpBasePort();
        _mrcpServer = new MrcpServerSocket(config.getMrcpPort());
        _config = config;
    }

    /* (non-Javadoc)
     * @see com.onomatopia.cairo.server.resource.Resource#invite(com.onomatopia.cairo.server.resource.ResourceMessage)
     */
    public ResourceMessage invite(ResourceMessage request) throws ResourceUnavailableException {
        
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
                        RTPSpeechSynthChannel promptPlayer = new RTPSpeechSynthChannel(localPort, remoteHost, remotePort);
                        _mrcpServer.openChannel(channelID, new MrcpSpeechSynthChannel(channelID, promptPlayer, _basePromptDir));
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
        URL configURL = new URL(args[0]);
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
        //Thread.sleep(90000);
    }

}
