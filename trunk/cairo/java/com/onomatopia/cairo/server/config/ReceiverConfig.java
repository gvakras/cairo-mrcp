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
package com.onomatopia.cairo.server.config;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.NoSuchElementException;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;

/**
 * Class encapsulating all configuration information for a receiver resource.
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
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
