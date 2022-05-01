package simulation;//import sim.engine.*;
//import sim.util.*;

import java.util.*;
import java.util.stream.Collectors;

import base.Action;
import base.Context;
import base.Location;
import base.Norm;
import base.Relationship;
import ec.util.MersenneTwisterFast;
import lcs.ActionSelectionStrategy;
import lcs.AlternatingExplorationStrategy;
import lcs.AlwaysMatchingMutationStrategy;
import lcs.BaseLCS;
import lcs.ButzFitnessWeightedActionSelection;
import lcs.CoveringStrategy;
import lcs.DistinctActionsBasedCoveringStrategy;
import lcs.EpsilonGreedyExplorationStrategy;
import lcs.ExplorationStrategy;
import lcs.FitnessWeightedActionSelection;
import lcs.InitialPeriodExplorationStrategy;
import lcs.KovacsDeletionScheme;
import lcs.LCSAlgorithm;
import lcs.SingleRuleCoveringStrategy;
import lcs.TournamentParentSelection;
import lcs.UniformCrossoverStrategy;
import rulebasedrl.BaseNormProvider;
import rulebasedrl.BaseNormProviderWithoutDefaultNorm;
import rulebasedrl.HardCodedCaseNormProvider;
import rulebasedrl.MatchingSet;
import rulebasedrl.NormEntry;
import rulebasedrl.NormProvider;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.util.Bag;
import statebasedrl.StateRLAlgoBaseGreedyReward;
import statebasedrl.StateRLAlgorithm;
import util.Randomizer;
import weka.classifiers.Classifier;
import weka.classifiers.functions.LinearRegression;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

/**
 *
 * @author Hui
 */
public class Agent implements Steppable {
    
    public int remainingSteps = 0;
    public double callRate = 0.05;
    public int location = -1;

    public AgentType agentType = AgentType.PERFECT;
    public int familyCircle = -1;
    public int colleagueCircle = -1;
    public int friendCircle = -1;
    
    public Bag myFamilies = new Bag();
    public Bag myColleagues = new Bag();
    public Bag myFriends = new Bag();
    public Bag myStrangers = new Bag();
    
    //neighboring calls to which this agent needs to give feedbacks
    public Bag neighboringCalls = new Bag();
    
    //history of calls this agent receives (as a callee)
    //not used for now
    public Bag callHistory = new Bag();
    
    //history of records for classification
    //public Bag records = new Bag();
    
    //Weka dataset
    public Instances data;

    //No. of instances learnt in reinforcement learning
    public int rlDataInstancesCount;

    //current neighbors;
    public Bag currentNeighbors = new Bag();
    
    //lock to avoid being called twice in a step. 
    public boolean isCalled = false;
    public Call currentCall = null;
    
    public int id = -1;

    public List<NormEntry> internalNorms;
    private StateRLAlgorithm stateRLAlgorithm;
    public LCSAlgorithm lcsAlgorithm;

    private static double LEARNING_RATE = 0.9;

    private NormProvider normProvider;

    public Agent(){
        location = -1;
        remainingSteps = 0;
        this.id = -1;
        initDataset();
        this.internalNorms = new ArrayList<>();
        this.rlDataInstancesCount = 0;
        this.normProvider = getNormProvider();
    }

    public Agent(int id, AgentType type){
        this.agentType = type;
        location = -1;
        remainingSteps = 0;
        this.id = id;
        initDataset();
        this.internalNorms = new ArrayList<>();
        this.rlDataInstancesCount = 0;
        this.normProvider = getNormProvider();
    }

    public Agent(int id){
        location = -1;
        remainingSteps = 0;
        this.id = id;
        initDataset();
        this.internalNorms = new ArrayList<>();
        this.rlDataInstancesCount = 0;
        this.normProvider = getNormProvider();
    }

    private static NormProvider getNormProvider() {
//        return new BaseNormProvider();
        if (Agents.simulationNumber == 4) {
            return new BaseNormProvider();
        } else if (Agents.simulationNumber == 7 || Agents.simulationNumber == 5) {
            return new BaseNormProviderWithoutDefaultNorm();
        }
        return null;
    }

    public void setAgentType(AgentType agentType) {
        this.agentType = agentType;
    }

    public static Instances getEmptyDataset() {
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
        aa = new Attribute("answer", tf);
        alist.add(aa);
        //payoff, numeric
        aa = new Attribute("@Class@");
        alist.add(aa);

        Instances dataset = new Instances("Call Record", alist, 0);
        dataset.setClassIndex(dataset.numAttributes()-1);
        return dataset;
    }

    //Initialize the Weka dataset
    public void initDataset(){
        this.data = getEmptyDataset();
    }

    //Use record for RL learning
    public void addRecordRL(RecordForLearning rec, Call call) {
        double reward = rec.getPayoff();
        Action calleeAction = Action.fromID(call.action);
        MatchingSet matchingSet = call.matchingSet;
//        if (matchingSet == null) {
//            System.err.println("MatchingSet is null");
//        }
        List<Norm> matchingNorms = matchingSet.normEntries.stream().map(en -> en.norm).collect(Collectors.toList());
        Map<Norm, Double> normToReward = new HashMap<>();
        for (Norm norm : matchingNorms) {
            if (norm.consequent == calleeAction) {
                normToReward.put(norm, reward);
            } else {
                normToReward.put(norm, -reward);
            }
        }
        for (NormEntry entry : this.internalNorms) {
            if (normToReward.containsKey(entry.norm)) {
                entry.updateWeight(getUpdatedWeight(entry.weight, normToReward.get(entry.norm)));
            }
        }
        this.rlDataInstancesCount++;
    }

    public void addRecordStateRL(RecordForLearning rec, Call call) {
        double reward = rec.getPayoff();
        Context fullContext = Context.builder()
                .calleeLocation(Location.get(call.location / Agents.numAgents))
                .callerRelationship(getCallerRelationship(call))
                .callUrgency(call.urgency)
                .build();
        Action calleeAction = Action.fromID(call.action);
        stateRLAlgorithm.learn(fullContext, calleeAction, reward);
        this.rlDataInstancesCount++;
    }

    public void addRecordLCS(RecordForLearning rec, Call call) {
        double reward = rec.getPayoff();
        Context fullContext = Context.builder()
                .calleeLocation(Location.get(call.location / Agents.numAgents))
                .callerRelationship(getCallerRelationship(call))
                .callUrgency(call.urgency)
                .build();
        Action calleeAction = Action.fromID(call.action);
        lcsAlgorithm.learn(fullContext, calleeAction, reward);
        this.rlDataInstancesCount++;
    }

    private double getUpdatedWeight(double oldWeight, double reward) {
        return oldWeight + LEARNING_RATE * (reward - oldWeight);
    }

    //Add a record to Weka dataset
    public void addRecord(RecordForLearning rec){
        double[] one = new double[data.numAttributes()];
        
        //location
        one[0] = rec.location;
        //caller relation
        one[1] = rec.callerRelation;
        //urgency
        one[2] = rec.urgency?0:1;//note that 0 is for true
        //exists family
        one[3] = rec.existsFamily?0:1;
        //exists colleague
        one[4] = rec.existsColleague?0:1;
        //exists friend
        one[5] = rec.existsFriend?0:1;
        //answer or not?
        one[6] = 1-rec.action;
        //payoff
        one[7] = rec.getPayoff();
        
        data.add(new DenseInstance(1.0, one));
    }
    
    public void step(SimState state){
        
        Agents agents = (Agents)state;
        double x = 0.0;
        
        //Enter a random place
        if (remainingSteps<=0){
            int sum = 0;
            int i;
            for(i=0;i<agents.locationWeights.length; i++){
                sum+=agents.locationWeights[i];
            }
            x = agents.random.nextDouble();
            int y = 0;
            for(i=0;i<agents.locationWeights.length;i++){
                y += agents.locationWeights[i];
                if (x<=(double)y/(double)sum)
                    break;
            }
            if (i>=agents.locationWeights.length)
                i=agents.locationWeights.length;
            
            //location = loction type id * number of agents + location id
            //e.g., meeting #1 with 1000 agents = 1*1000+1=1001
            //75% probability, agent enters own home/meeting/party
            //25% probability, agent enters another random home/meeting/party
            x = agents.random.nextDouble();
            switch(i){
                case 0: //home
                    x = x*agents.numHomes*4;
                    if (x>=agents.numHomes)
                        location = i*agents.numAgents+familyCircle;
                    else
                        location = i*agents.numAgents+(int)x;
                    break;
                case 1: //meeting
                    x = x*agents.numMeetings*4;
                    if (x>=agents.numMeetings)
                        location = i*agents.numAgents+colleagueCircle;
                    else
                        location = i*agents.numAgents+(int)x;
                    break;
                case 2: //party
                    x = x*agents.numParties*4;
                    if (x>=agents.numParties)
                        location = i*agents.numAgents+friendCircle;
                    else
                        location = i*agents.numAgents+(int)x;
                    break;
                default:
                    location = i*agents.numAgents;
            }
            remainingSteps = (int)(agents.random.nextGaussian()*30+60.5);
            if (remainingSteps>90) remainingSteps = 90;
            if (remainingSteps<30) remainingSteps = 30;
            remainingSteps *= agents.locationWeights[i];
            
        }else{
            remainingSteps --;
        }
        
        //Output one agent's info
        /*
        if (this.id==0){
            System.out.println("Location: "+agents.locations[location/agents.numAgents]
                    +" #"+(location%agents.numAgents));
            System.out.println("Remaining: "+remainingSteps);
        }*/
        
        //Once every agent enters a place...
        if (state.schedule.getSteps()<=0) return;
        
        //As a caller
        //Randomly make a random call
        x = agents.random.nextDouble();
        if (x<=callRate){
            
            //25% agent calls family, 25% colleague, 25% friend, 25% stranger
            x = agents.random.nextDouble();
            Bag temp;
            if (x<0.25)
                temp = this.myFamilies;
            else if (x<0.5)
                temp = this.myColleagues;
            else if (x<0.75)
                temp = this.myFriends;
            else
                temp = this.myStrangers;
            
            x = agents.random.nextDouble();
            Agent callee = (Agent)temp.get((int)(x*temp.size()));
            //Caller and callee should not be in the same place
            while(callee.location==this.location){
                temp = this.myStrangers;//to avoid all group members being in the same place
                x = agents.random.nextDouble();
                callee = (Agent)temp.get((int)(x*temp.size()));
            }
            
            /*
            Agent callee= (Agent)agents.allAgents.get((int)(x*agents.numAgents));
            //Caller and callee should not be in the same place
            while(callee.location==this.location){
                x = agents.random.nextDouble();
                callee= (Agent)agents.allAgents.get((int)(x*agents.numAgents));
            }*/
            
            //make sure that each agent is only called once in each step
            if (!callee.isCalled){
                x = agents.random.nextDouble();
                Call call = new Call(this, callee, x<0.5, state.schedule.getSteps());

                //The callee makes a decision (whether or not to take this call).
                agents.callerLocationStats.add(Location.get(this.location / Agents.numAgents), 1);
                callee.handleACall(call, state);
                agents.callsInThisStep.add(call);
                agents.allCalls.add(call);

                //Keep history. Disabled for now to save space
                //callee.callHistory.add(call);
                
                callee.isCalled = true;
                callee.currentCall = call;

                //Add this call to all of the callee's neighbors
                callee.currentNeighbors = agents.getNeighbors(callee.location);
                Agent neighbor;
                for(int i=0;i<callee.currentNeighbors.size();i++){
                    neighbor = (Agent)callee.currentNeighbors.get(i);
                    if (neighbor.id!=callee.id)
                        neighbor.neighboringCalls.add(call);
                }
            }
        }
        
        //As a neigbhor
        //Respond to neighbor calls with feedbacks. 
        //Move this step to after all agents have made a call. 
        //giveFeedbacks(state);
    }
    
    public void handleACall(Call call, SimState state){
        long startTime = 0;
        if (Agents.MEASURE_TIME) {
            startTime = System.nanoTime();
        }
        Agents agents = (Agents)state;
        call.action = getFixedAction(call, state);

        call.explanationStatistics = agents.explanationStatistics;
        agents.callRelationshipStats.add(getCallerRelationship(call), 1);
        agents.calleeLocationStats.add(Location.get(call.location / Agents.numAgents), 1);

        //A better way to make a decision, with adaptive learning, 
        //is using Weka's classification methods.
        //Learning starts after a learning period
        if ((Agents.simulationNumber>=2 && Agents.simulationNumber < 4)&&(this.data.numInstances()>Agents.learningPeriod)){
            int temp = getAction(call,state);
            if (temp>=0)
                call.action = temp;
        }

        Context callContext = Context.builder()
                .callUrgency(call.urgency)
                .calleeLocation(Location.get(call.location / Agents.numAgents))
                .callerRelationship(getCallerRelationship(call))
                .build();
        call.privacy = callContext.getPrivacy();

        //Explicit norms approach
        //if ((Agents.simulationNumber>=4)&&(this.data.numInstances()>Agents.learningPeriod)){
        if (Agents.isRuleBasedRLCase()) {
            DecisionResponse decisionResponse = getActionForExplicitNorms(callContext, state);
            int temp = rlDataInstancesCount > Agents.learningPeriod ? decisionResponse.action.id : call.action;
            //int temp = this.data.numInstances() > Agents.learningPeriod ? decisionResponse.action.id : call.action;
            if (temp>=0) {
                call.action = temp;
                call.explanation = decisionResponse.explanation;
                //call.explanation = rlDataInstancesCount > Agents.learningPeriod ? decisionResponse.explanation : null;
//                System.err.println("Matching set is set");
                call.matchingSet = decisionResponse.matchingSet;
                if (rlDataInstancesCount > Agents.learningPeriod) {
                    call.privacy = decisionResponse.matchingSet.getDeducedContext().getPrivacy();
                }
                //call.explanationStatistics = agents.explanationStatistics;
            }
        }

        if (Agents.simulationNumber == 6 && rlDataInstancesCount > Agents.learningPeriod) {
            Action action = stateRLAlgorithm.getDecision(callContext);
            call.action = action.id;
        }

        if (Agents.isLCS()) {
            Action action = lcsAlgorithm.getDecision(callContext);
            call.action = action.id;
            if (Agents.isLCSNewExplanation()) {
                call.lcsNewExplanation = lcsAlgorithm.explainDecision(callContext, action);
                call.privacy = getPrivacyScoreFromLCSExplanation(call.lcsNewExplanation);
            }
        }

        if (Action.fromID(call.action) == Action.IGNORE)  {
            agents.lastIgnoreStep = Math.max(agents.lastIgnoreStep, state.schedule.getSteps());
        }

        if (getCallerRelationship(call) == Relationship.STRANGER && !call.urgency) {
            agents.lastExceptionCaseStep = Math.max(agents.lastExceptionCaseStep, state.schedule.getSteps());
        }

        if (Agents.MEASURE_TIME) {
            call.timeToDecide = System.nanoTime() - startTime;
        }
    }

    private double getPrivacyScoreFromLCSExplanation(List<Norm> lcsNewExplanation) {
        Context context = Context.builder().build();
        for (Norm norm : lcsNewExplanation) {
            context.mergeFrom(norm.getConditions());
        }
        return context.getPrivacy();
    }
    
    public void giveFeedbacks(SimState state) {
        Agents agents = (Agents)state;
        Bag todo = new Bag(neighboringCalls);
        neighboringCalls = new Bag();
        if (todo.size()<=0)
            return;
        Call call;
        Feedback temp;
        
        //boolean feedback = true;
        //UPDATE: Now we use payoff instead of boolean feedback;
        double payoff = 0.0;
        
        for(int i=0;i<todo.size();i++){
            call = (Call)todo.get(i);
            long startTime = 0;
            if (Agents.MEASURE_TIME) {
                startTime = System.nanoTime();
            }

            //decide a feedback based on call info
            //One solution is that the feedback is always random:
            //feedback = state.random.nextBoolean();
            
            //To begin with, let's assume that agents give feedbacks
            //solely based on locations:
            //positive if answered at home, parties, diner; random if ignored
            //negative if answered at meeting or library; random if ignored
            
            /*
            if ((int)(location/Agents.numAgents)==1||(int)(location/Agents.numAgents)==3){
                if (call.action==1)
                    feedback = false;
                else
                    feedback = state.random.nextBoolean();
            }
            else{
                if (call.action==1)
                    feedback = true;
                else
                    feedback = state.random.nextBoolean();
            }*/
            
            
            //UPDATED: now we use payoffs as feedbacks
            //In the 2nd simulation: 
            //remeber that, in meeting, library and party, 
            //people think callee should ignore
            int l = (int)(call.location/Agents.numAgents);
//            if (call.action==1){
//                switch(l){
//                    case 1:
//                        payoff = agents.payoff_i[12+2*l];
//                        break;
//                    case 2:
//                        payoff = agents.payoff_i[12+2*l];
//                        break;
//                    case 3:
//                        payoff = agents.payoff_i[12+2*l];
//                        break;
//                    default:
//                        payoff = agents.payoff_a[12+2*l];
//                        break;
//                }
//            }
//            else{
//                switch(l){
//                    case 1:
//                        payoff = agents.payoff_i[13+2*l];
//                        break;
//                    case 2:
//                        payoff = agents.payoff_i[13+2*l];
//                        break;
//                    case 3:
//                        payoff = agents.payoff_i[13+2*l];
//                        break;
//                    default:
//                        payoff = agents.payoff_a[13+2*l];
//                        break;
//                }
//            }

            Location calleeLocation = Location.get(l);
            Action calleeAction = Action.fromID(call.action);
            boolean accept = calleeAction == Action.RING
                    ? (calleeLocation == Location.HOME || calleeLocation == Location.ER)
                    : (calleeLocation == Location.MEETING || calleeLocation == Location.LIBRARY
                        || calleeLocation == Location.PARTY);

            //In the 3rd simulation,
            //neighbor hears the explanation
            if ((Agents.simulationNumber==3)&&(this.data.numInstances()>Agents.learningPeriod)){
                //get the action that the neighbor would take
                int action = getAction(call, state);
                accept = action == call.action;
            }

            //In the 4rd simulation,
            //neighbor hears the explanation as per the explicit norms using RL approach
            //if ((Agents.simulationNumber==4)&&(this.data.numInstances()>Agents.learningPeriod)){
//            if (((Agents.simulationNumber==4) || (Agents.simulationNumber == 5) || Agents.simulationNumber == 7)
            if (Agents.isRuleBasedRLCaseListenExplanation()
                    &&(this.rlDataInstancesCount > Agents.learningPeriod)
                    && call.explanation != null){
                Explanation explanation = call.explanation;
                accept = explanation.accept(this, calleeAction,
                        Context.builder().calleeLocation(Location.get(call.location / Agents.numAgents)).build());
            }

            Context fullContext = Context.builder()
                                        .calleeLocation(calleeLocation)
                                        .callerRelationship(getCallerRelationship(call))
                                        .callUrgency(call.urgency)
                                        .build();

            if (Agents.isStateRL() && this.rlDataInstancesCount > Agents.learningPeriod) {
                List<Action> validActions = stateRLAlgorithm.getDecisions(fullContext);
                accept = validActions.contains(calleeAction);
            }

            if (Agents.isLCSNoExplanationOwnNorms() && this.rlDataInstancesCount > Agents.learningPeriod) {
                Context neighborContext = Context.builder().calleeLocation(calleeLocation).build();
                List<Action> validActions = lcsAlgorithm.getAcceptableDecisions(neighborContext);
                accept = validActions.contains(calleeAction);
            }

            if (Agents.isLCSListenExplanation() && this.rlDataInstancesCount > Agents.learningPeriod) {
                List<Action> validActions;
                if (Agents.isLCSNewExplanationPlusOwnContext()) {
                    Context neighborContext = Context.builder().calleeLocation(calleeLocation).build();
                    validActions = lcsAlgorithm.getAcceptableDecisions(call.lcsNewExplanation, neighborContext);
                } else if (Agents.isLCSNewExplanation()) {
                    validActions = lcsAlgorithm.getAcceptableDecisions(call.lcsNewExplanation);
                } else {
                    validActions = lcsAlgorithm.getAcceptableDecisions(fullContext);
                }
                accept = validActions.contains(calleeAction);
            }

            if (accept) {
                call.explanationStatistics.addAccept(calleeAction, calleeLocation);
            } else {
                call.explanationStatistics.addReject(calleeAction, calleeLocation);
            }
            payoff = agents.payoffCalculator.calculateNeighborPayoff(
                    Context.builder().calleeLocation(calleeLocation).build(), calleeAction, accept);

            //temp = new Feedback(call, this, feedback);
            temp = new Feedback(call, this, payoff);
            call.feedbacks.add(temp);
            if (Agents.MEASURE_TIME) {
                call.timeToGiveFeedbacks += System.nanoTime() - startTime;
            }
        }
    }

    public String getLocationString(){
        return Agents.locations[location/Agents.numAgents]
                +" #"+(location%Agents.numAgents);
    }

    private int getFixedAction(Call call, SimState state) {
        Agents agents = (Agents)state;

        //Agent will make a decision based on call info,
        //as well as call history
        //do adaptive learning based on feedbacks.

        //decision being whether or not to answer the call
        //0 for ignored, 1 for answered.

        //One method is that the action is always random.
        //call.action = state.random.nextBoolean()?1:0;

        /*
        To begin with, let's assume the agents comply with the following norms:
        -- Answer calls if the agent is at home, parties or diner, and
        -- Ignore calls otherwise(meeting or library)
        -- Ignore calls if casual from strangers, answer otherwise.
        */
        boolean basedonloc = true;
        boolean basedoncall = true;

        //Originally, the fixed norms are:
        //If in a meeting or a library, not answer
        //Otherwise, answer
        if ((int)(location/Agents.numAgents)==1||(int)(location/Agents.numAgents)==3){
            basedonloc = false;
        }

        //Later, we decided to use the following norms:
        //  -- If in a meeting or a library, definitely ignore;
        //  -- If in an ER, definitely answer;
        //  -- If at home, more likely to answer(67% answer vs 33% ignore);
        //  -- If at a party, more likely to ignore(33% answer vs 67% ignore)

        double x = state.random.nextDouble();
        switch((int)(location/Agents.numAgents)){
            //at home
            case 0:
                basedonloc = x<0.67;
                break;
            //in a meeting
            case 1:
                basedonloc = false;
                break;
            //at a party
            case 2:
                basedonloc = x>0.67;
                break;
            //in a library
            case 3:
                basedonloc = true;
                break;
            //in an ER
            case 4:
                basedonloc = false;
                break;
            default: break;
        }

        double calleePayoffForRing = agents.payoffCalculator
                .calculateCalleePayoff(Context.builder()
                        .callUrgency(call.urgency)
                        .callerRelationship(getCallerRelationship(call))
                        .build(), Action.RING);
        double calleePayoffForIgnore = agents.payoffCalculator
                .calculateCalleePayoff(Context.builder()
                        .callUrgency(call.urgency)
                        .callerRelationship(getCallerRelationship(call))
                        .build(), Action.IGNORE);

        basedoncall = calleePayoffForRing > calleePayoffForIgnore;

        //basedoncall is based on Callee Payoff,
        // (choose the action with higher payoff)
        //which is the same as:
        //if (call.isStranger()&&(!call.urgency))
        //    basedoncall = false;
//        if (call.isStranger()){
//            if (call.urgency)
//                basedoncall = agents.payoff_a[6]>agents.payoff_a[7];
//            else
//                basedoncall = agents.payoff_a[4]>agents.payoff_a[5];
//        }
//        else{
//            if (call.urgency)
//                basedoncall = agents.payoff_a[2]>agents.payoff_a[3];
//            else
//                basedoncall = agents.payoff_a[0]>agents.payoff_a[1];
//        }

        if (basedonloc==basedoncall) {
            //call.action = basedonloc?1:0;
            return basedonloc ? 1 : 0;
        }
        else {
            //call.action = state.random.nextBoolean()?1:0;
            return state.random.nextBoolean()?1:0;
        }
    }

    private List<Integer> getFixedAction(List<Call> calls, SimState state) {
        return calls.stream().map(call -> getFixedAction(call, state)).collect(Collectors.toList());
    }

    public List<Integer> getBulkAction(List<Call> calls, SimState state) {
        if (Agents.simulationNumber == 1) {
            return getFixedAction(calls, state);
        }
        Agents agents = (Agents)state;
        int action = -1;
        Classifier cls = new LinearRegression();
        List<Integer> actions = new ArrayList<>();
        try {
            cls.buildClassifier(data);
            for (Call call : calls) {
                RecordForLearning rec = new RecordForLearning(call, agents, this);
                double[] one = new double[data.numAttributes()];
                //location
                one[0] = rec.location;
                //caller relation
                one[1] = rec.callerRelation;
                //urgency
                one[2] = rec.urgency ? 0 : 1;//note that 0 is for true
                //exists family
                one[3] = rec.existsFamily ? 0 : 1;
                //exists colleague
                one[4] = rec.existsColleague ? 0 : 1;
                //exists frienpayoffd
                one[5] = rec.existsFriend ? 0 : 1;
                //What if I ignore?
                one[6] = 1;
                //payoff
                one[7] = 0;//0 for now

                try {
                    //What if I ignore?
                    one[6] = 1;
                    double a1 = cls.classifyInstance(new DenseInstance(1.0, one));
                    //What if I answer?
                    one[6] = 0;
                    double a2 = cls.classifyInstance(new DenseInstance(1.0, one));

                    //choose the action with higher predicted overall payoff
                    if (a1 > a2)
                        action = 0;
                    else
                        action = 1;
                    actions.add(action);
                } catch (Exception e) {
                    //do nothing
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return actions;
    }

    //Decided, based on history, which action is better
    public int getAction(Call call, SimState state){
        Agents agents = (Agents)state;
        int action = -1;
        Classifier cls = new LinearRegression();
        RecordForLearning rec = new RecordForLearning(call, agents, this);
        double[] one = new double[data.numAttributes()];
        //location
        one[0] = rec.location;
        //caller relation
        one[1] = rec.callerRelation;
        //urgency
        one[2] = rec.urgency?0:1;//note that 0 is for true
        //exists family
        one[3] = rec.existsFamily?0:1;
        //exists colleague
        one[4] = rec.existsColleague?0:1;
        //exists friend
        one[5] = rec.existsFriend?0:1;
        //What if I ignore?
        one[6] = 1;
        //payoff
        one[7] = 0;//0 for now

        try{
            cls.buildClassifier(data);
            //What if I ignore?
            one[6] = 1;
            double a1 = cls.classifyInstance(new DenseInstance(1.0, one));
            //What if I answer?
            one[6] = 0;
            double a2 = cls.classifyInstance(new DenseInstance(1.0, one));

            //choose the action with higher predicted overall payoff
            if (a1>a2)
                action = 0;
            else
                action = 1;
        }
        catch(Exception e){
            //do nothing
        }
        return action;
    }

    public DecisionResponse getActionForExplicitNorms(Context callContext, SimState simState) {
        //Agents agents = (Agents) simState;
        MatchingSet matchingSet = getMatchingSetForExplicitNorms(callContext);
        List<Action> decisions = matchingSet.getDecisions();
        Action action = decisions.contains(Action.RING) ? Action.RING : Action.IGNORE;
        //base.Action action = decisions.get(agents.random.nextInt(decisions.size()));
        List<Norm> matchingNorms = matchingSet.normEntries.stream().map(en -> en.norm).collect(Collectors.toList());
        Explanation explanation = new OrderedNormExplanation(matchingNorms);
        return new DecisionResponse(action, explanation, matchingSet);
    }

    private static class DecisionResponse {
        public Action action;
        public Explanation explanation;
        public MatchingSet matchingSet;

        public DecisionResponse(Action action, Explanation explanation, MatchingSet matchingSet) {
            this.action = action;
            this.explanation = explanation;
            this.matchingSet = matchingSet;
        }
    }

    public MatchingSet getMatchingSetForExplicitNorms(Context context) {
        MatchingSet matchingSet = new MatchingSet();
        for (NormEntry entry : this.internalNorms) {
            if (entry.norm.triggers(context)) {
//                Debugger.debug(entry, "Triggering entry");
                matchingSet.add(entry);
            }
        }
        return matchingSet;
    }

    //start inclusive, end exclusive
    private static int getRandom(int start, int end, SimState state) {
        return start + state.random.nextInt(end - start);
    }

    public static Relationship getCallerRelationship(Call call) {
        if (call.isFamily()) return Relationship.FAMILY;
        if (call.isFriend()) return Relationship.FRIEND;
        if (call.isColleague()) return Relationship.COLLEAGUE;
        return Relationship.STRANGER;
    }

    public void initializeNorms() {
        this.internalNorms = normProvider.provide();
    }

    public void initStateRLAlgo(int simulationNumber, SimState state) {
        //Add simulationNumber checks
        this.stateRLAlgorithm = new StateRLAlgoBaseGreedyReward(state.random);
    }

    public void initLCS(SimState state) {
        //Add simulationNumber checks
        BaseLCS.Parameters parameters = new BaseLCS.Parameters();
        parameters.maxPopulation = 30;
        Randomizer randomizer = new Randomizer(state.random);
        this.lcsAlgorithm = new BaseLCS(
                state.random,
//                new FitnessWeightedActionSelection(state.random),
                getActionSelectionStrategy(state.random),
                getCoveringStrategy(randomizer, parameters.dontCareProb),
                new TournamentParentSelection(randomizer),
                new UniformCrossoverStrategy(randomizer, parameters.crossoverBitSwapProbability),
                new AlwaysMatchingMutationStrategy(randomizer, parameters.mutationProb),
                new KovacsDeletionScheme(randomizer, parameters.experienceThresholdForDeletion,
                        parameters.maxPopulation, parameters.fitnessThreshold),
                getLCSExplorationStrategy(randomizer),
                parameters
        );
    }

    private ActionSelectionStrategy getActionSelectionStrategy(MersenneTwisterFast random) {
        if (Agents.simulationNumber >= 14) {
            return new ButzFitnessWeightedActionSelection(random);
        } else {
            return new FitnessWeightedActionSelection(random);
        }
    }

    private CoveringStrategy getCoveringStrategy(Randomizer randomizer, double dontCareProb) {
        if (Agents.simulationNumber >= 13) {
            return new DistinctActionsBasedCoveringStrategy(randomizer, dontCareProb);
        } else {
            return new SingleRuleCoveringStrategy(randomizer, dontCareProb);
        }
    }

    private ExplorationStrategy getLCSExplorationStrategy(Randomizer randomizer) {
        if (Agents.simulationNumber == 8 || Agents.simulationNumber == 9) {
            return new AlternatingExplorationStrategy();
        } else if (Agents.simulationNumber == 10) {
            return new EpsilonGreedyExplorationStrategy(randomizer);
        } else if (Agents.simulationNumber == 11 || Agents.simulationNumber >= 13) {
            return new InitialPeriodExplorationStrategy(new EpsilonGreedyExplorationStrategy(randomizer),
                    100);
        } else if (Agents.simulationNumber == 12) {
            return new InitialPeriodExplorationStrategy(new AlternatingExplorationStrategy(), 100);
        }
        throw new RuntimeException();
    }

    public void contributeNorms(Map<Norm, Double> voteMap) {
        lcsAlgorithm.voteNorms(voteMap);
    }
}
