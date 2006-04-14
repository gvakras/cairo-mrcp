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
package org.speechforge.cairo.server.config;

import java.io.File;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;

/**
 * Class encapsulating all configuration information for a transmitter resource.
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
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
