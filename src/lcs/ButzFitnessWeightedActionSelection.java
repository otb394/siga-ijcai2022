package lcs;

import base.Action;
import ec.util.MersenneTwisterFast;
import util.Debugger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ButzFitnessWeightedActionSelection implements ActionSelectionStrategy {
    private MersenneTwisterFast random;

    public ButzFitnessWeightedActionSelection(MersenneTwisterFast random) {
        this.random = random;
    }

    @Override
    public List<Action> acceptableActions(RuleSet matchSet, boolean debug) {
        Map<Action, Double> actionToNum = new HashMap<>();
        Map<Action, Double> actionToDenom = new HashMap<>();
        Action[] actions = Action.values();
        for (Action action : actions) {
            for (Rule rule : matchSet.getRules()) {
                if (rule.norm.consequent == action) {
                    double num = actionToNum.getOrDefault(action, 0.0);
                    num += rule.fitness * rule.rewardPrediction;
                    actionToNum.put(action, num);
                    double denom = actionToDenom.getOrDefault(action, 0.0);
                    denom += rule.fitness;
                    actionToDenom.put(action, denom);
                }
            }
        }
        List<Action> possibleActions = new ArrayList<>();
        double curr = Double.NEGATIVE_INFINITY;
        for (Action action : actions) {
//            double vote = actionToVote.getOrDefault(action, Double.NEGATIVE_INFINITY);
            Double num = actionToNum.get(action);
            if (num == null) continue;
            double denom = actionToDenom.getOrDefault(action, 1.0);
            double vote = (denom == 0) ? num : (num / denom);
            if (vote > curr) {
                possibleActions.clear();;
                possibleActions.add(action);
                curr = vote;
            } else if (vote == curr) {
                possibleActions.add(action);
            }
        }
//        if (debug && possibleActions.contains(Action.IGNORE)) {
//            Debugger.debug(possibleActions, "possibleActions");
//            matchSet.print();
//        }
        return possibleActions;
    }

    @Override
    public Action selectAction(RuleSet matchSet, boolean debug) {
        List<Action> possibleActions = acceptableActions(matchSet, debug);
        return possibleActions.get(random.nextInt(possibleActions.size()));
    }
}
