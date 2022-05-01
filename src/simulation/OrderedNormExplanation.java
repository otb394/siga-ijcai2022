package simulation;

import base.Action;
import base.Context;
import base.Norm;
import rulebasedrl.MatchingSet;

import java.util.List;

public class OrderedNormExplanation extends Explanation {
    public List<Norm> norms;
    public Context deducedContext;

    public OrderedNormExplanation(List<Norm> norms) {
        this.norms = norms;
        this.deducedContext = Context.builder().build();
        for (Norm norm : norms) {
            deducedContext.mergeFrom(norm.getConditions());
        }
    }

    @Override
    public boolean accept(Agent agent, Action calleeAction, Context neighborContext) {
        Context finalContext = deducedContext.merge(neighborContext);
        MatchingSet matchingSet = agent.getMatchingSetForExplicitNorms(finalContext);
        List<Action> decisions = matchingSet.getDecisions();
        return decisions.contains(calleeAction);
    }
}
