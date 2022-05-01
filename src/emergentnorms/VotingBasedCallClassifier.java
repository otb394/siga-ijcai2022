package emergentnorms;

import ec.util.MersenneTwisterFast;
import sim.engine.SimState;
import sim.util.Bag;
import simulation.Agent;
import simulation.Agents;
import simulation.Call;
import simulation.RecordForLearning;
import util.Debugger;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VotingBasedCallClassifier {
    private List<Agent> agents;
    private Agents agentsState;
    private MersenneTwisterFast random;

    public VotingBasedCallClassifier(Agents state, Bag agents) {
        this.agentsState = state;
        //this.state = state;
        //private SimState state;
        int numAgents = agents.size();
        this.agents = new ArrayList<>(numAgents);
        for (int agentIndex = 0; agentIndex < numAgents; agentIndex++) {
            this.agents.add((Agent)agents.get(agentIndex));
        }
        this.random = state.random;
    }

    private Instances getDataset() {
        //Attribute list
        ArrayList<Attribute> alist = new ArrayList<Attribute>();
        Attribute aa;

        //location
        aa = new Attribute("location", new ArrayList<String>(
                Arrays.asList(Agents.locations)));
        alist.add(aa);

        //caller relation
        aa = new Attribute("caller_relation", new ArrayList<String>(
                Arrays.asList(RecordForLearning.relationTypes)));
        alist.add(aa);

        //urgency
        List<String> tf = new ArrayList<String>();//True of False attributes
        tf.add("true");tf.add("false");
        aa = new Attribute("urgency", tf);
        alist.add(aa);
        //exists_family
        aa = new Attribute("exists_family", tf);
        alist.add(aa);
        //exists_colleague
        aa = new Attribute("exists_colleague", tf);
        alist.add(aa);
        //exists_friend
        aa = new Attribute("exists_friend", tf);
        alist.add(aa);
        //answer or not
        aa = new Attribute("@Class@", tf);
        alist.add(aa);

        //payoff, numeric
        //aa = new Attribute("@Class@");
        //alist.add(aa);

        Instances dataset = new Instances("Sample calls", alist, 0);
        dataset.setClassIndex(dataset.numAttributes()-1);
        return dataset;
    }

    private Instances getLabeledInstances(List<Call> callList) {
        int numCalls = callList.size();
        Instances instances = getDataset();
        Map<Integer, List<Integer>> agentIndexToActions = new HashMap<>();
        int numAgents = agents.size();
        int answerAction = 0;
        int ignoreAction = 0;
        for (int agentIndex = 0; agentIndex < numAgents; agentIndex++) {
            Agent agent = agents.get(agentIndex);
            List<Integer> actions = agent.getBulkAction(callList, agentsState);
            for (int action : actions) {
                if (action == 1) {
                    answerAction++;
                } else {
                    ignoreAction++;
                }
            }
            agentIndexToActions.put(agentIndex, actions);
        }
        int answerCount = 0;
        int ignoreCount = 0;
        int callAnswerCount = 0;
        int callIgnoreCount = 0;
        double maxIgnoreCountPercet = 0.0;
        double minIgnoreCountPercentage = 1.0;
        for (int callIndex = 0; callIndex < numCalls; callIndex++) {
            Call call = callList.get(callIndex);
            if (call.action == 1) {
                callAnswerCount++;
            } else {
                callIgnoreCount++;
            }
            int answerVote = 0;
            int ignoreVote = 0;
            for (int agentIndex = 0; agentIndex < numAgents; agentIndex++) {
                int action = agentIndexToActions.get(agentIndex).get(callIndex);
                if (action == 1) {
                    answerVote++;
                } else {
                    ignoreVote++;
                }
            }
            double ignorePercentage = ((double) ignoreVote) / (ignoreVote + answerVote);
            //Debugger.debug(ignorePercentage, "ignorePercentage", answerVote, "answerVote", ignoreVote, "ignoreVote");
            maxIgnoreCountPercet = Math.max(maxIgnoreCountPercet, ignorePercentage);
            minIgnoreCountPercentage = Math.min(minIgnoreCountPercentage, ignorePercentage);
            int popularAction = answerVote >= ignoreVote ? 1 : 0;
            //int popularAction = (answerVote > 10*ignoreVote) ? 1 : 0;
            //int popularAction = (ignorePercentage < 0.1) ? 1 : 0;
            if (popularAction == 1) {
                answerCount++;
            } else {
                ignoreCount++;
            }
            instances.add(getInstance(call, popularAction, instances));
        }
        Debugger.debug(answerCount, "answerCount", ignoreCount, "ignoreCount",
                callAnswerCount, "callAnswerCount", callIgnoreCount, "callIgnoreCount",
                maxIgnoreCountPercet, "max ignore vote percentage",
                minIgnoreCountPercentage, "min ignore vote percentage",
                answerAction, "answer actions",
                ignoreAction, "ignore actions");
        return instances;
    }

    public Instances getLabeledInstances(Bag calls) {
        int numCalls = calls.size();
        List<Call> ignoredCalls = new ArrayList<>();
        List<Call> answeredCalls = new ArrayList<>();
        for (int callIndex = 0; callIndex < numCalls; callIndex++) {
            Call call = (Call) calls.get(callIndex);
            if (call.action == 1) {
                answeredCalls.add(call);
            } else {
                ignoredCalls.add(call);
            }
            //callList.add((Call) calls.get(callIndex));
        }
        Debugger.debug(answeredCalls.size(), "answerCallCountInData", ignoredCalls.size(), "ignoredCallCountInData");
        List<Call> callList = new ArrayList<>(ignoredCalls);
        int ignoreNum = ignoredCalls.size();
        //int takeNum = Math.min(ignoreNum, answeredCalls.size());
        int takeNum = answeredCalls.size();
        List<Call> answeredCallSample = sample(answeredCalls, takeNum);
        Debugger.debug(takeNum, "takeNum", answeredCallSample.size(), "answered call sample size");
        callList.addAll(answeredCallSample);

        return getLabeledInstances(callList);
    }

    private <T> List<T> sample(List<T> list, int num) {
        int[] perm = getRandomPermutation(num);
        List<T> ret = new ArrayList<>();
        for (int i = 0; i < num; i++) {
            ret.add(list.get(perm[i]));
        }
        return ret;
    }

    /**
     * Generates a random permutation of numbers from 1 to n-1, both inclusive
     */
    private int[] getRandomPermutation(int n) {
        int[] arr = new int[n];
        for (int i = 0; i < n; i++) {
            arr[i] = i;
        }
        for (int i = n; i > 1; i--) {
            swap(arr, i-1, random.nextInt(i));
        }
        return arr;
    }


    private void swap(int[] arr, int i, int j) {
        int temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }

    private Instance getInstance(Call call, int action, Instances dataset) {
        double[] row = new double[dataset.numAttributes()];
        RecordForLearning rec = new RecordForLearning(call, agentsState, agents.get(0));
        //location
        row[0] = rec.location;
        //caller relation
        row[1] = rec.callerRelation;
        //urgency
        row[2] = rec.urgency?0:1;//note that 0 is for true
        //exists family
        row[3] = rec.existsFamily?0:1;
        //exists colleague
        row[4] = rec.existsColleague?0:1;
        //exists friend
        row[5] = rec.existsFriend?0:1;
        //answer or not?
        row[6] = 1.0-action;

        return new DenseInstance(1.0, row);
    }
}
