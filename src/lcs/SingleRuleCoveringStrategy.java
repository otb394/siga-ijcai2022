package lcs;

import base.Action;
import base.Context;
import base.Norm;
import util.Randomizer;

import java.util.ArrayList;
import java.util.List;

public class SingleRuleCoveringStrategy implements CoveringStrategy {
    private Randomizer randomizer;
    private double dontCareProb;

    public SingleRuleCoveringStrategy(Randomizer randomizer, double dontCareProb) {
        this.randomizer = randomizer;
        this.dontCareProb = dontCareProb;
    }

    @Override
    public boolean initiateCovering(RuleSet matchSet) {
        return matchSet.isEmpty();
    }

    @Override
    public List<Rule> getCoveringRules(Context context, RuleSet matchSet) {
        Rule coveringRule = getRandomCoveringMatchingRule(context);
        List<Rule> coveringRules = new ArrayList<>();
        coveringRules.add(coveringRule);
        return coveringRules;
    }

    private Rule getRandomCoveringMatchingRule(Context context) {
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
        Context randomMatchingContext = Context.fromVector(randomVector);
        Action randomAction = randomizer.random.nextBoolean() ? Action.RING : Action.IGNORE;
        Norm randomNorm = new Norm(randomMatchingContext, randomAction);
        return new Rule(randomNorm);
    }
}
