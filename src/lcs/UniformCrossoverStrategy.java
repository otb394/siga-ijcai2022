package lcs;

import base.Action;
import base.Context;
import base.Norm;
import util.Randomizer;

import java.util.ArrayList;
import java.util.List;

public class UniformCrossoverStrategy implements CrossoverStrategy {
    private double crossoverBitSwapProbability;
    private Randomizer randomizer;

    public UniformCrossoverStrategy(Randomizer randomizer, double crossoverBitSwapProbability) {
        this.crossoverBitSwapProbability = crossoverBitSwapProbability;
        this.randomizer = randomizer;
    }

    @Override
    public Rule[] crossover(Rule firstParent, Rule secondParent) {
        List<Object> firstParentVector = firstParent.norm.getConditions().getVector();
        List<Object> secondParentVector = secondParent.norm.getConditions().getVector();
        List<Object> firstChildVector = new ArrayList<>();
        List<Object> secondChildVector = new ArrayList<>();
        int vectorSize = firstParentVector.size();
        for (int i = 0; i < vectorSize; i++) {
            if (randomizer.random.nextBoolean(crossoverBitSwapProbability)) {
                firstChildVector.add(secondParentVector.get(i));
                secondChildVector.add(firstParentVector.get(i));
            } else {
                firstChildVector.add(firstParentVector.get(i));
                secondChildVector.add(secondParentVector.get(i));
            }
        }
        Action action = firstParent.norm.consequent;
        Rule firstChild = new Rule(new Norm(Context.fromVector(firstChildVector), action));
        Rule secondChild = new Rule(new Norm(Context.fromVector(secondChildVector), action));
        return new Rule[]{firstChild, secondChild};
    }
}
