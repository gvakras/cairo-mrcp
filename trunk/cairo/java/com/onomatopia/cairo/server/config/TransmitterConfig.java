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
public class TransmitterConfig extends ResourceConfig {

    private File _basePromptDir;

    /**
     * TODOC
     * @param index
     * @param config
     * @throws ConfigurationException 
     */
    public TransmitterConfig(int index, XMLConfiguration config) throws ConfigurationException {
        super(index, config);
        try {
            _basePromptDir = new File(config.getString("resources.resource(" + index + ").basePromptDir"));
            ensureDir(_basePromptDir);
        } catch (RuntimeException e) {
            throw new ConfigurationException(e.getMessage(), e);
        }
    }


    /**
     * TODOC
     * @return Returns the basePromptDir.
     */
    public File getBasePromptDir() {
        return _basePromptDir;
    }

}
