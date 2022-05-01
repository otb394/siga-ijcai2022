package lcs;

import base.Context;
import base.Norm;
import util.Randomizer;

import java.util.ArrayList;
import java.util.List;

public class AlwaysMatchingMutationStrategy implements MutationStrategy {
    private Randomizer randomizer;
    private double mutationProbability;

    public AlwaysMatchingMutationStrategy(Randomizer randomizer, double mutationProbability) {
        this.randomizer = randomizer;
        this.mutationProbability = mutationProbability;
    }

    @Override
    public Rule mutate(Rule rule, Context situation) {
        List<Object> ruleVector = rule.norm.getConditions().getVector();
        List<Object> mutatedVector = new ArrayList<>();
        List<Object> situationVector = situation.getVector();
        int vectorSize = ruleVector.size();
        for (int i = 0; i < vectorSize; i++) {
            Object ruleElement = ruleVector.get(i);
            if (randomizer.random.nextBoolean(mutationProbability)) {
                if (ruleElement == null) {
                    mutatedVector.add(situationVector.get(i));
                } else {
                    mutatedVector.add(null);
                }
            } else {
                mutatedVector.add(ruleElement);
            }
        }
        Rule mutatedRule = new Rule(rule);
        mutatedRule.setNorm(new Norm(Context.fromVector(mutatedVector), rule.getNorm().consequent));
        return mutatedRule;
    }
}
