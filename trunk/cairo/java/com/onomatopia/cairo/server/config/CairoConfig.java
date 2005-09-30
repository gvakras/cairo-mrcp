/**
 * TODOC
 * 
 */
package com.onomatopia.cairo.server.config;

import java.net.URL;
import java.util.List;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;

/**
 * TODOC
 * @author Niels
 *
 */
public class CairoConfig {

    XMLConfiguration _config;

    /**
     * TODOC
     * @param cairoConfigURL 
     * @throws ConfigurationException 
     */
    public CairoConfig(URL cairoConfigURL) throws ConfigurationException {
        _config = new XMLConfiguration(cairoConfigURL);
    }

    /**
     * TODOC
     * @param name
     * @return
     * @throws ConfigurationException
     */
    public ReceiverConfig getReceiverConfig(String name) throws ConfigurationException {
        return new ReceiverConfig(this.getConfigIndex(name), _config);
    }

    /**
     * TODOC
     * @param name
     * @return
     * @throws ConfigurationException
     */
    public TransmitterConfig getTransmitterConfig(String name) throws ConfigurationException {
        return new TransmitterConfig(this.getConfigIndex(name), _config);
    }

    private int getConfigIndex(String name) throws ConfigurationException {
        List<String> resourceNames = _config.getList("resources.resource.name");
        for (int i = 0; i < resourceNames.size(); i++) {
            String resourceName = resourceNames.get(i);
            if (resourceName.equalsIgnoreCase(name)) {
                return i;
            }
        }
        throw new ConfigurationException("Specified name not found in configuration!");
    }
}
