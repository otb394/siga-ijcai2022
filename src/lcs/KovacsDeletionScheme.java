package lcs;

import util.Debugger;
import util.Randomizer;

import java.util.ArrayList;
import java.util.List;

public class KovacsDeletionScheme implements DeletionStrategy {
    private Randomizer randomizer;
    private int deletionThreshold;
    private int maxPopulationSize;
    private double fitnessThreshold;

    public KovacsDeletionScheme(Randomizer randomizer, int deletionThreshold, int maxPopulationSize,
                                double fitnessThreshold) {
        this.randomizer = randomizer;
        this.deletionThreshold = deletionThreshold;
        this.maxPopulationSize = maxPopulationSize;
        this.fitnessThreshold = fitnessThreshold;
    }

    @Override
    public boolean prune(RuleSet ruleSet) {
        int totalNumerosity = 0;
        List<Rule> rules = ruleSet.getRules();
        int uniqueRuleSize = rules.size();
        double totalFitness = 0.0;
        for (Rule rule : rules) {
            totalNumerosity += rule.numerosity;
            totalFitness += rule.fitness * rule.numerosity;
        }
        if (totalNumerosity <= maxPopulationSize) {
            return false;
        }
        double meanFitness = totalFitness / totalNumerosity;

        double[] weights = new double[uniqueRuleSize];
        int z = 0;

        for (Rule rule : rules) {
            double weight = rule.actionSetSize * rule.numerosity;
            if (weight == 0) {
                Debugger.debug(rule.actionSetSize, "actionSetSize", rule.numerosity, "numerosity");
            }

            boolean sufficientExperience = rule.experience > deletionThreshold;
            boolean lowFitness = rule.fitness < (fitnessThreshold * meanFitness);
            if (sufficientExperience && lowFitness) {
                weight *= meanFitness / rule.fitness;
            }
            if (weight == 0) {
                Debugger.debug(meanFitness, "meanFitness", rule.fitness, "rule.fitness");
            }
            weights[z++] = weight;
        }

        Rule ruleToBeDeleted = randomizer.choice(rules, weights);
        ruleSet.removeRule(ruleToBeDeleted);
        return true;
    }
}
