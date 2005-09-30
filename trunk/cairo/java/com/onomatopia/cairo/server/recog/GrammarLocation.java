/**
 * TODOC
 * 
 */
package com.onomatopia.cairo.server.recog;

import java.net.URL;

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