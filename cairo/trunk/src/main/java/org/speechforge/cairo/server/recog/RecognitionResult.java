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
package org.speechforge.cairo.server.recog;

import org.speechforge.cairo.util.rule.RuleMatch;
import org.speechforge.cairo.util.rule.SimpleNLRuleHandler;

import java.util.List;

import javax.speech.recognition.GrammarException;
import javax.speech.recognition.RuleGrammar;
import javax.speech.recognition.RuleParse;

import edu.cmu.sphinx.result.Result;

import org.apache.log4j.Logger;

/**
 * Represents the result of a completed recognition request.
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 */
public class RecognitionResult {

    private static Logger _logger = Logger.getLogger(RecognitionResult.class);

    private Result _rawResult;
    private RuleGrammar _ruleGrammar;
    private String _text;
    private List<RuleMatch> _ruleMatches;


    /**
     * TODOC
     * @param rawResult
     * @throws NullPointerException
     */
    public RecognitionResult(Result rawResult, RuleGrammar ruleGrammar) throws NullPointerException {
        _rawResult = rawResult;
        _ruleGrammar = ruleGrammar;
        if (_rawResult != null) {
            _text = _rawResult.getBestFinalResultNoFiller();
            if (_text != null && (_text = _text.trim()).length() > 0 && _ruleGrammar != null) {
                try {
                    RuleParse ruleParse = _ruleGrammar.parse(_text, null);
                    _ruleMatches = SimpleNLRuleHandler.getRuleMatches(ruleParse);
                } catch (GrammarException e) {
                    _logger.warn("GrammarException encountered!", e);
                }
            }
        }
    }

    /**
     * TODOC
     * @return Returns the original result.
     */
    public Result getRawResult() {
        return _rawResult;
    }

    /**
     * TODOC
     * @return
     */
    public RuleGrammar getRuleGrammar() {
        return _ruleGrammar;
    }

    /**
     * TODOC
     * @return
     */
    public String getText() {
        return _text;
    }

    /**
     * TODOC
     * @return
     */
    public List<RuleMatch> getRuleMatches() {
        return _ruleMatches;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(_text);
        if (_ruleMatches != null) {
            for (RuleMatch ruleMatch : _ruleMatches) {
                sb.append('<').append(ruleMatch.getRule());
                sb.append(':').append(ruleMatch.getTag()).append('>');
            }
        }
        return sb.toString();
    }

}
