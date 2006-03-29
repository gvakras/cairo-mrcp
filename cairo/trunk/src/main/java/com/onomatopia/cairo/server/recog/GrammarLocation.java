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
package com.onomatopia.cairo.server.recog;

import java.net.URL;

/**
 * TODOC
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 *
 */
public class GrammarLocation {

    public static final String DEFAULT_EXTENSION = "gram";

    URL _baseURL;
    String _grammarName;
    String _extension;

    public GrammarLocation(URL baseURL, String grammarName) {
        this(baseURL, grammarName, DEFAULT_EXTENSION);
    }

    public GrammarLocation(URL baseURL, String grammarName, String extension) {
        _baseURL = baseURL;
        _grammarName = grammarName;
        _extension = extension;
    }

    /**
     * TODOC
     * @return Returns the baseURL.
     */
    public URL getBaseURL() {
        return _baseURL;
    }

    /**
     * TODOC
     * @return Returns the grammarName.
     */
    public String getGrammarName() {
        return _grammarName;
    }

    /**
     * TODOC
     * @return Returns the extension.
     */
    public String getExtension() {
        return _extension;
    }

    /**
     * TODOC
     * @return Returns the extension.
     */
    public String getFilename() {
        return new StringBuilder(_grammarName).append('.').append(_extension).toString();
    }


}