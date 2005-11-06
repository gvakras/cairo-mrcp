/**
 * TODOC
 * 
 */
package com.onomatopia.cairo.server.config;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.NoSuchElementException;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;

/**
 * TODOC
 * @author Niels
 *
 */
public class ReceiverConfig extends ResourceConfig {

    private int _recEngines;
    private File _baseGrammarDir;
    private URL _sphinxConfigURL;
    private File _recordingDir;

    /**
     * TODOC
     * @param index
     * @param config
     * @throws ConfigurationException 
     */
    public ReceiverConfig(int index, XMLConfiguration config) throws ConfigurationException {
        super(index, config);
        try {
            try {
                _sphinxConfigURL = new URL(config.getString("resources.resource(" + index + ").sphinxConfigURL"));
            } catch (NoSuchElementException e) {
                _sphinxConfigURL = this.getClass().getResource("config/sphinx-config.xml");
                if (_sphinxConfigURL == null) {
                    throw e;
                }
            }
            _recEngines = config.getInt("resources.resource(" + index + ").recEngines");
            _baseGrammarDir = new File(config.getString("resources.resource(" + index + ").baseGrammarDir"));
            ensureDir(_baseGrammarDir);
            _recordingDir = new File(config.getString("resources.resource(" + index + ").recordingDir"));
            ensureDir(_recordingDir);
        } catch (RuntimeException e) {
            throw new ConfigurationException(e.getMessage(), e);
        } catch (MalformedURLException e) {
            throw new ConfigurationException(e.getMessage(), e);
        }
    }


    /**
     * TODOC
     * @return Returns the baseGrammarDir.
     */
    public File getBaseGrammarDir() {
        return _baseGrammarDir;
    }

    /**
     * TODOC
     * @return Returns the recordingDir.
     */
    public File getRecordingDir() {
        return _recordingDir;
    }

    /**
     * TODOC
     * @return Returns the recEngines.
     */
    public int getRecEngines() {
        return _recEngines;
    }


    /**
     * TODOC
     * @return Returns the sphinxConfigURL.
     */
    public URL getSphinxConfigURL() {
        return _sphinxConfigURL;
    }

}
