package lcs;

import base.Context;

import java.util.List;

public interface CoveringStrategy {
//    boolean initiateCovering(List<Rule> matchSet);
    boolean initiateCovering(RuleSet matchSet);
    List<Rule> getCoveringRules(Context context, RuleSet matchSet);
}
