/**
 * TODOC
 * 
 */
package com.onomatopia.cairo.server.recog;

import edu.cmu.sphinx.result.Result;

/**
 * TODOC
 * @author Niels
 *
 */
public class RecognitionResult {

    private Result _result;

    /**
     * TODOC
     */
    public RecognitionResult(Result result) {
        _result = result;
    }

    /**
     * TODOC
     * @return Returns the result.
     */
    public Result getResult() {
        return _result;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return (_result == null) ? null : _result.getBestFinalResultNoFiller();
    }

}
