/**
 * TODOC
 * 
 */
package com.onomatopia.cairo.server.config;

import java.io.File;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;

/**
 * TODOC
 * @author Niels
 *
 */
public abstract class ResourceConfig {

    private int _mrcpPort;
    private int _rtpBasePort;
    private int _maxConnects;

    /**
     * TODOC
     * @param index 
     * @param config 
     */
    public ResourceConfig(int index, XMLConfiguration config) {
        _mrcpPort = config.getInt("resources.resource(" + index + ").mrcpPort");
        _rtpBasePort = config.getInt("resources.resource(" + index + ").rtpBasePort");
        _maxConnects = config.getInt("resources.resource(" + index + ").maxConnects");
    }

    /**
     * TODOC
     * @return Returns the maxConnects.
     */
    public int getMaxConnects() {
        return _maxConnects;
    }

    /**
     * TODOC
     * @return Returns the mrcpPort.
     */
    public int getMrcpPort() {
        return _mrcpPort;
    }

    /**
     * TODOC
     * @return Returns the rtpBasePort.
     */
    public int getRtpBasePort() {
        return _rtpBasePort;
    }

    public static void ensureDir(File dir) throws ConfigurationException {
        
        // create directory if it does not exist
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new ConfigurationException(
                    "Could not create directory: " + dir.getAbsolutePath());
            }
        }

        // make sure dir is actually a directory
        if (!dir.isDirectory()) {
            throw new ConfigurationException(
                "File specified was not a directory: " + dir.getAbsolutePath());
        }
    }

}
