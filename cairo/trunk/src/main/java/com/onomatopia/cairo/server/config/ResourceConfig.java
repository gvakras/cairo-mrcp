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

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;

/**
 * Base class for specific resource type configurations.
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
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
