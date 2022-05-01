package statebasedrl;

import base.Action;
import base.Context;
import ec.util.MersenneTwisterFast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class StateRLAlgoBaseGreedyReward implements StateRLAlgorithm {
    public Map<Context, Map<Action, Double>> valueMap;
    private MersenneTwisterFast random;

    //Hyperparameter
    private static final double STEP_SIZE = 0.9;

    public StateRLAlgoBaseGreedyReward(MersenneTwisterFast random) {
        this.valueMap = new HashMap<>();
        this.random = random;
    }

    private double getReward(Context context, Action action) {
        return Optional.ofNullable(valueMap.get(context))
                       .map(mp -> mp.getOrDefault(action, getDefault()))
                       .orElse(getDefault());
    }

    private double getDefault() {
        return 0.0;
    }

    @Override
    public Action getDecision(Context context) {
        List<Action> decisions = getDecisions(context);
        return selectFromSimilarActions(decisions);
    }

    private Action selectFromSimilarActions(List<Action> actions) {
        if (random != null) {
            return actions.get(random.nextInt(actions.size()));
        } else {
            return actions.get(0);
        }
    }

    @Override
    public void learn(Context context, Action action, double reward) {
        double oldReward = getReward(context, action);
        double newReward = oldReward + STEP_SIZE * (reward - oldReward);
        Map<Action, Double> stateValueMap = Optional.ofNullable(valueMap.get(context)).orElseGet(HashMap::new);
        stateValueMap.put(action, newReward);
        valueMap.put(context, stateValueMap);
    }

    @Override
    public List<Action> getDecisions(Context context) {
        List<Action> decisions = new ArrayList<>();
        double curr = Double.NEGATIVE_INFINITY;
        Action[] actions = Action.values();
        for (Action action : actions) {
            double reward = getReward(context, action);
            //Debugger.debug(reward, "reward", curr, "curr", decisions, "decisions");
            if (reward > curr) {
                curr = reward;
                decisions.clear();
                decisions.add(action);
            } else if (reward == curr) {
                decisions.add(action);
            }
        }
//        if (decisions.isEmpty()) {
//            Debugger.debug(context, "context", valueMap, "valueMap");
//            throw new RuntimeException();
//        }
        return decisions;
    }
}
