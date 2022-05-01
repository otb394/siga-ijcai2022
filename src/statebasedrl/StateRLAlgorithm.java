package statebasedrl;

import base.Action;
import base.Context;

import java.util.List;

public interface StateRLAlgorithm {
    Action getDecision(Context context);
    void learn(Context context, Action action, double reward);
    List<Action> getDecisions(Context context);
}
