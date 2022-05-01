package lcs;

import base.Action;
import base.Context;
import base.Norm;
import ec.util.MersenneTwisterFast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class BaseLCS implements LCSAlgorithm {
    private RuleSet population;
    private MersenneTwisterFast random;
    private ActionSelectionStrategy actionSelectionStrategy;
    private CoveringStrategy coveringStrategy;
    private Parameters parameters;
    private ParentSelectionStrategy parentSelectionStrategy;
    private CrossoverStrategy crossoverStrategy;
    private MutationStrategy mutationStrategy;
    private DeletionStrategy deletionStrategy;
    private ExplorationStrategy explorationStrategy;

//    private boolean debug = false;
//    private int temp = 0;
//    private int maxTemp = 200;

    public BaseLCS(MersenneTwisterFast random,
                   ActionSelectionStrategy actionSelectionStrategy,
                   CoveringStrategy coveringStrategy,
                   ParentSelectionStrategy parentSelectionStrategy,
                   CrossoverStrategy crossoverStrategy,
                   MutationStrategy mutationStrategy,
                   DeletionStrategy deletionStrategy,
                   ExplorationStrategy explorationStrategy) {
        this.population = new RuleSet(this::prune);
        this.parameters = new Parameters();
        this.random = random;
        this.actionSelectionStrategy = actionSelectionStrategy;
        this.coveringStrategy = coveringStrategy;
        this.parentSelectionStrategy = parentSelectionStrategy;
        this.crossoverStrategy = crossoverStrategy;
        this.mutationStrategy = mutationStrategy;
        this.deletionStrategy = deletionStrategy;
        this.explorationStrategy = explorationStrategy;
    }

    public BaseLCS(MersenneTwisterFast random,
                   ActionSelectionStrategy actionSelectionStrategy,
                   CoveringStrategy coveringStrategy,
                   ParentSelectionStrategy parentSelectionStrategy,
                   CrossoverStrategy crossoverStrategy,
                   MutationStrategy mutationStrategy,
                   DeletionStrategy deletionStrategy,
                   ExplorationStrategy explorationStrategy,
                   Parameters parameters) {
//        this.population = new ArrayList<>();
        this.population = new RuleSet(this::prune);
        this.parameters = parameters;
        this.random = random;
        this.actionSelectionStrategy = actionSelectionStrategy;
        this.coveringStrategy = coveringStrategy;
        this.parentSelectionStrategy = parentSelectionStrategy;
        this.crossoverStrategy = crossoverStrategy;
        this.mutationStrategy = mutationStrategy;
        this.deletionStrategy = deletionStrategy;
        this.explorationStrategy = explorationStrategy;
    }

    @Override
    public Action getDecision(Context context) {
//        temp++;
//        if (!debug && temp > maxTemp) {
//            debug = true;
//        }
        if (explorationStrategy.explore()) {
            if (random.nextBoolean()) {
                return Action.RING;
            } else {
                return Action.IGNORE;
            }
        } else {
            RuleSet matchSet = population.getMatchSet(context);
            if (coveringStrategy.initiateCovering(matchSet)) {
                List<Rule> coveringRules = coveringStrategy.getCoveringRules(context, matchSet);
                matchSet.addAll(coveringRules);
                this.population.addAll(coveringRules);
            }
            return actionSelectionStrategy.selectAction(matchSet, false);
        }
    }

    @Override
    public void voteNorms(Map<Norm, Double> voteMap) {
        List<Rule> rules = this.population.getRules();
        for (Rule rule : rules) {
            double existingVote = voteMap.getOrDefault(rule.getNorm(), 0.0);
            voteMap.put(rule.getNorm(), existingVote + rule.fitness * rule.rewardPrediction);
        }
    }

    @Override
    public List<Norm> explainDecision(Context context, Action action) {
        RuleSet matchSet = population.getMatchSet(context);
        RuleSet actionSet = matchSet.getActionSet(action);
        return actionSet.getRules().stream().map(Rule::getNorm).collect(Collectors.toList());
    }

    @Override
    public void learn(Context context, Action action, double reward) {
        RuleSet matchSet = population.getMatchSet(context);
        RuleSet actionSet = matchSet.getActionSet(action);
        updateRuleParameters(actionSet, reward);
        if (parameters.doActionSetSubSumption) {
            actionSetSubSumption(actionSet);
        }
        geneticExploration(actionSet, context);
    }

    private void actionSetSubSumption(RuleSet actionSet) {
        List<Rule> actionRules = actionSet.getRules();
        int selectedRuleBitCount = -1;
        List<Rule> selectedRules = new ArrayList<>();
        for (Rule rule : actionRules) {
            if (!(rule.experience > parameters.experienceThresholdForSubSumption
                    && rule.errorOfPrediction < parameters.accuracyThreshold)) {
                continue;
            }
            int bitCount = rule.getNumberOfKnownBits();
            if (bitCount > selectedRuleBitCount) {
                selectedRules.clear();
                selectedRules.add(rule);
                selectedRuleBitCount = bitCount;
            } else if (bitCount == selectedRuleBitCount) {
                selectedRules.add(rule);
            }
        }
        if (selectedRules.isEmpty()) return;
        Rule selectedRule = selectedRules.get(random.nextInt(selectedRules.size()));

        for (Rule rule : actionRules) {
            if (!rule.equals(selectedRule) && selectedRule.norm.triggers(rule.norm.getConditions())) {
                selectedRule.numerosity += rule.numerosity;
                this.population.removeRule(rule, rule.numerosity);
                actionSet.removeRule(rule, rule.numerosity);
            }
        }
    }

    private void geneticExploration(RuleSet actionSet, Context situation) {
        if (actionSet.getAvgExperienceSinceRD() <= parameters.gaThreshold) {
            return;
        }
        actionSet.resetExperienceSinceRD();
        Rule firstParent = parentSelectionStrategy.select(actionSet);
        Rule secondParent = parentSelectionStrategy.select(actionSet);

        Rule firstChild, secondChild;

        if (random.nextBoolean(parameters.crossoverProbability)) {
            Rule[] children = crossoverStrategy.crossover(firstParent, secondParent);
            firstChild = children[0];
            secondChild = children[1];
        } else {
            firstChild = firstParent;
            secondChild = secondParent;
        }
        firstChild = mutationStrategy.mutate(firstChild, situation);
        secondChild = mutationStrategy.mutate(secondChild, situation);

        Rule[] children = new Rule[]{firstChild, secondChild};
        Rule[] parents = new Rule[]{firstParent, secondParent};
        for (Rule child : children) {
            if (parameters.doGASubSumption) {
                boolean subsumed = false;
                for (Rule parent : parents) {
                    boolean shouldSubsume = parent.experience > parameters.experienceThresholdForSubSumption
                            && parent.errorOfPrediction < parameters.accuracyThreshold
                            && parent.getNorm().triggers(child.getNorm().getConditions());
                    if (shouldSubsume) {
                        Rule copyParent = new Rule(parent);
                        copyParent.numerosity = 1;
                        this.population.addRule(copyParent);
                        subsumed = true;
                        break;
                    }
                }
                if (subsumed) continue;
            }

            if (!this.population.contains(child)) {
                child.fitness = (firstParent.fitness + secondParent.fitness) / 2.0;
                child.errorOfPrediction = (firstParent.errorOfPrediction + secondParent.errorOfPrediction) / 2.0;
                child.rewardPrediction = (firstParent.rewardPrediction + secondParent.rewardPrediction) / 2.0;
            }

            this.population.addRule(child);
        }
    }

    private void updateRuleParameters(RuleSet actionSet, double reward) {
        List<Double> rawAccuracies = new ArrayList<>();
        double accuracySum = 0.0;
        List<Rule> actionRules = actionSet.getRules();
        int totalNumerosity = 0;
        for (Rule rule : actionRules) {
            totalNumerosity += rule.numerosity;
            rule.incrementExperience();
            rule.rewardPrediction = rule.rewardPrediction + parameters.betaLearningRate
                    * (reward - rule.rewardPrediction);
            rule.errorOfPrediction = rule.errorOfPrediction
                    + parameters.betaLearningRate * (Math.abs(reward - rule.rewardPrediction) - rule.errorOfPrediction);
            double accuracy = (rule.errorOfPrediction < parameters.accuracyThreshold)
                    ? 1.0
                    : (parameters.fitnessFalloffAlpha
                    * Math.pow(rule.errorOfPrediction / parameters.accuracyThreshold, -parameters.fitnessExponent));
            rawAccuracies.add(accuracy);
            accuracySum += accuracy * rule.numerosity;
        }
//        double accuracySum = rawAccuracies.stream().mapToDouble(i -> i).sum();
        int uniqueRuleSize = actionRules.size();
        for (int index = 0; index < uniqueRuleSize; index++) {
            double rawAccuracy = rawAccuracies.get(index);
            Rule rule = actionRules.get(index);
//            double normalizedAccuracy = rawAccuracy / accuracySum;
            double normalizedAccuracy = (rawAccuracy * rule.numerosity) / accuracySum;
            rule.fitness = rule.fitness + parameters.betaLearningRate * (normalizedAccuracy - rule.fitness);

            rule.actionSetSize += parameters.betaLearningRate * (totalNumerosity - rule.actionSetSize);
        }
    }

    public void prune() {
        deletionStrategy.prune(this.population);
    }

    @Override
    public void printStats() {
        this.population.print();
    }

    @Override
    public List<Action> getAcceptableDecisions(Context context, boolean acceptingByDefault) {
        RuleSet matchSet = this.population.getMatchSet(context);
        if (matchSet.isEmpty()) {
            if (acceptingByDefault) {
                return Arrays.stream(Action.values()).collect(Collectors.toList());
            } else {
                return new ArrayList<>();
            }
        }
        return actionSelectionStrategy.acceptableActions(matchSet, false);
    }

    @Override
    public List<Action> getAcceptableDecisions(Context context) {
        return getAcceptableDecisions(context, true);
    }

    @Override
    public List<Action> getAcceptableDecisions(List<Norm> arguments) {
        return getAcceptableDecisions(arguments, true);
    }

    @Override
    public List<Action> getAcceptableDecisions(List<Norm> arguments, Context ownContext) {
        return getAcceptableDecisions(arguments, ownContext, true);
    }

    @Override
    public List<Action> getAcceptableDecisions(List<Norm> arguments, Context ownContext, boolean acceptingByDefault) {
        Set<Rule> applicableRules = new LinkedHashSet<>();
        for (Norm argument : arguments) {
            RuleSet matchSet = this.population.getMatchSet(argument.getConditions());
            List<Rule> matchingRules = matchSet.getRules();
            applicableRules.addAll(matchingRules);
        }
        RuleSet ownMatchSet = this.population.getMatchSet(ownContext);
        List<Rule> ownRules = ownMatchSet.getRules();
        applicableRules.addAll(ownRules);
        RuleSet combinedMatchSet = new RuleSet(new ArrayList<>(applicableRules));
        if (combinedMatchSet.isEmpty()) {
            if (acceptingByDefault) {
                return Arrays.stream(Action.values()).collect(Collectors.toList());
            } else {
                return new ArrayList<>();
            }
        }
        return actionSelectionStrategy.acceptableActions(combinedMatchSet, false);
    }

    @Override
    public List<Action> getAcceptableDecisions(List<Norm> arguments, boolean acceptingByDefault) {
        Set<Rule> applicableRules = new LinkedHashSet<>();
        for (Norm argument : arguments) {
            RuleSet matchSet = this.population.getMatchSet(argument.getConditions());
            List<Rule> matchingRules = matchSet.getRules();
            applicableRules.addAll(matchingRules);
        }
        RuleSet combinedMatchSet = new RuleSet(new ArrayList<>(applicableRules));
        if (combinedMatchSet.isEmpty()) {
            if (acceptingByDefault) {
                return Arrays.stream(Action.values()).collect(Collectors.toList());
            } else {
                return new ArrayList<>();
            }
        }
        return actionSelectionStrategy.acceptableActions(combinedMatchSet, false);
    }

    public static class Parameters {
        public double betaLearningRate;
        public int maxPopulation;
        public double dontCareProb;
        public double accuracyThreshold;
        public double fitnessExponent;
        public double fitnessFalloffAlpha;
        public int gaThreshold;
        public boolean doActionSetSubSumption;
        public int experienceThresholdForSubSumption;
        public double crossoverProbability;
        public double crossoverBitSwapProbability;
        public boolean doGASubSumption;
        public int experienceThresholdForDeletion;
        public double fitnessThreshold;
        public double mutationProb;

        public Parameters() {
            this.betaLearningRate = 0.1;
            this.maxPopulation = 1000;
            this.dontCareProb = 0.3;
            this.accuracyThreshold = 0.01;
            this.fitnessExponent = 5.0;
            this.fitnessFalloffAlpha = 0.1;
            this.gaThreshold = 25;
            this.doActionSetSubSumption = true;
            this.experienceThresholdForSubSumption = 20;
            this.crossoverProbability = 0.8;
            this.crossoverBitSwapProbability = 0.5;
            this.doGASubSumption = true;
            this.experienceThresholdForDeletion = 20;
            this.fitnessThreshold = 0.1;
            this.mutationProb = 0.4;
        }
    }
}
