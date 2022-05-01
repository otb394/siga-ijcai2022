package lcs;

import util.Debugger;
import util.Randomizer;

import java.util.ArrayList;
import java.util.List;

public class TournamentParentSelection implements ParentSelectionStrategy {
    private static final double SELECTION_RATIO = 0.3;
    private Randomizer randomizer;

    public TournamentParentSelection(Randomizer randomizer) {
        this.randomizer = randomizer;
    }

    //TODO: use experience here: Not used in Butz paper
    @Override
    public Rule select(RuleSet ruleSet) {
        List<Rule> rules = ruleSet.getRules();
        int actionSetSize = rules.size();
//        Debugger.debug(actionSetSize, "actionSetSize");
        int tournamentSize = (int)(Math.ceil(SELECTION_RATIO * actionSetSize));
        List<Rule> tournament = randomizer.sample(rules, actionSetSize, tournamentSize);
        List<Rule> maximumRules = new ArrayList<>();
//        Debugger.debug(tournamentSize, "tournamentSize");
        double maxFitness = 0.0;
        for (Rule rule : tournament) {
            if (rule.fitness > maxFitness) {
                maximumRules.clear();
                maximumRules.add(rule);
                maxFitness = rule.fitness;
            } else if (rule.fitness == maxFitness) {
                maximumRules.add(rule);
            }
        }
        return maximumRules.get(randomizer.getRandom(0, maximumRules.size()));
    }
}
