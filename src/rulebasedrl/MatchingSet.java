package rulebasedrl;

import base.Action;
import base.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MatchingSet {
    public List<NormEntry> normEntries;

    public MatchingSet() {
        this.normEntries = new ArrayList<>();
    }

    public void add(NormEntry normEntry) {
        this.normEntries.add(normEntry);
    }

    public List<Action> getDecisions() {
        Action action = Action.RING;
        double currWeight = Double.NEGATIVE_INFINITY;
        List<Action> decisions = new ArrayList<>();
        for (NormEntry entry : this.normEntries) {
//            Debugger.debug(currWeight, "currWeight", entry, "entry", decisions, "decisions");
            if (entry.weight > currWeight) {
                decisions.clear();
                decisions.add(entry.norm.consequent);
                currWeight = entry.weight;
            } else if (entry.weight == currWeight) {
                decisions.add(entry.norm.consequent);
            }
        }
        return decisions.isEmpty() ? Collections.singletonList(action) : decisions;
    }

    public Context getDeducedContext() {
        Context deducedContext = Context.builder().build();
        for (NormEntry normEntry : this.normEntries) {
            deducedContext.mergeFrom(normEntry.norm.getConditions());
        }
        return deducedContext;
    }

    @Override
    public String toString() {
        return "MatchingSet{" +
                "normEntries=" + normEntries +
                '}';
    }
}
