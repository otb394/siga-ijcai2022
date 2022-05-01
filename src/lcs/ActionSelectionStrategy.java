package lcs;

import base.Action;

import java.util.List;

public interface ActionSelectionStrategy {
    Action selectAction(RuleSet matchSet, boolean printDebug);
    List<Action> acceptableActions(RuleSet matchSet, boolean printDebug);
}
