package lcs;

import base.Action;
import util.Randomizer;

import java.util.List;

public class FittestRuleActionSelectionStrategy implements ActionSelectionStrategy{
    private Randomizer randomizer;

    public FittestRuleActionSelectionStrategy(Randomizer randomizer) {
        this.randomizer = randomizer;
    }

    @Override
    public Action selectAction(RuleSet matchSet, boolean debug) {
        List<Action> possibleActions = acceptableActions(matchSet, debug);
        return possibleActions.get(randomizer.random.nextInt(possibleActions.size()));
    }

    @Override
    public List<Action> acceptableActions(RuleSet matchSet, boolean debug) {
        //TODO
        return null;
    }
}
