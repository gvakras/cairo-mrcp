package org.speechforge.cairo.util.rule;

public class RuleMatch {

    private String _rule;
    private String _tag;

    public RuleMatch(String rule, String tag) {
        _rule = rule;
        _tag = tag;
    }

    public String getRule() {
        return _rule;
    }

    public void setRule(String rule) {
        _rule = rule;
    }

    public String getTag() {
        return _tag;
    }

    public void setTag(String tag) {
        _tag = tag;
    }

}
