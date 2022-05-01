package lcs;

import base.Action;
import base.Context;
import base.Norm;
import util.Randomizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class DistinctActionsBasedCoveringStrategy implements CoveringStrategy {
    private Randomizer randomizer;
    private double dontCareProb;

    public DistinctActionsBasedCoveringStrategy(Randomizer randomizer, double dontCareProb) {
        this.randomizer = randomizer;
        this.dontCareProb = dontCareProb;
    }

    @Override
    public boolean initiateCovering(RuleSet matchSet) {
        return matchSet.getRules().stream()
                .map(rule -> rule.getNorm().consequent)
                .distinct()
                .count() < (Action.values().length);
    }

    @Override
    public List<Rule> getCoveringRules(Context context, RuleSet matchSet) {
        Set<Action> remainingActions = new LinkedHashSet<>(Arrays.asList(Action.values()));
        List<Rule> rules = matchSet.getRules();
        for (Rule rule : rules) {
            remainingActions.remove(rule.getNorm().consequent);
        }
        List<Action> remActionList = new ArrayList<>(remainingActions);
        List<Rule> coveringRules = new ArrayList<>();
        while (!remainingActions.isEmpty()) {
            Context randomContext = getRandomCoveringContext(context);
            Action randomAction = remActionList.get(randomizer.random.nextInt(remActionList.size()));
            remainingActions.remove(randomAction);
            coveringRules.add(new Rule(new Norm(randomContext, randomAction)));
        }
        return coveringRules;
    }

    private Context getRandomCoveringContext(Context context) {
        List<Object> vector = context.getVector();
        List<Object> randomVector = new ArrayList<>();
        for (Object element : vector) {
            boolean makeWild = randomizer.random.nextBoolean(dontCareProb);
            if (element == null || makeWild) {
                randomVector.add(null);
            } else {
                randomVector.add(element);
            }
        }
        return Context.fromVector(randomVector);
    }
}
