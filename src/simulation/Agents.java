package simulation;//import sim.engine.*;
//import sim.util.*;
//import sim.field.continuous.*;
//import sim.field.network.*;

import base.Action;
import base.Context;
import base.Location;
import base.Norm;
import base.Relationship;
import emergentnorms.VotingBasedCallClassifier;
import lcs.Rule;
import lcs.RuleSet;
import rulebasedrl.NormEntry;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.util.Bag;
import util.Debugger;
import weka.classifiers.trees.J48;
import weka.core.Instances;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * @author Hui
 */
public class Agents extends SimState {

    //Which simulation are we doing?
    public static int simulationNumber = 15;

    //Which payoff scheme is being used
    public static int payoffScheme = 2;

    public static int trial = 3;

    //Main parameters
    //There is an assumption in global location id calculator that numAgents >= numHomes, numMeetings, numParties
    //Changed from 1000,20,20,20
    public static int numAgents = 1000;
    public static int numHomes = 20;
    public static int numMeetings = 20;
    public static int numParties = 20;
//    public static int numAgents = 250;
//    public static int numHomes = 5;
//    public static int numMeetings = 5;
//    public static int numParties = 5;
    public static int[] agentSocietyRatios = new int[]{1, 0, 0};
    
    public static double callRate = 0.05;
    
    //Location names
    public static String[] locations = {"home", "meeting","party","library","ER"};

    //location weights are multipliers for probabilities and durations.
    //Use uniform weights for now. Originally {8,2,3,2,1}.
    public static int[] locationWeights = {1,1,1,1,1};
    
    //Agent will start to learn (instead of using fixed norms) 
    //after the learning period.
    public static int learningPeriod = 50;
    //public static int learningPeriod = 500;

    public static boolean MEASURE_TIME = false;

    private static final double NORM_EMERGENCE_THRESHOLD = 0.9;

    //Average the output in a window (#steps)
    public static int windowSize = 200;
    public static int windowSize2 = 200;

    public long lastIgnoreStep = 0;
    public long lastExceptionCaseStep = 0;

    public LinkedHashMap<Long, Double> window = new LinkedHashMap<Long, Double>(){
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Double> eldest){
            return this.size() > windowSize;
        }
    };
    
    public LinkedHashMap<Long, Integer> numCalls = new LinkedHashMap<Long, Integer>(){
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Integer> eldest){
            return this.size() > windowSize;
        }
    };
    
    public LinkedHashMap<Long, Double> window2 = new LinkedHashMap<Long, Double>(){
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Double> eldest){
            return this.size() > windowSize2;
        }
    };

    public Explanation.Statistics explanationStatistics;
    public RelationshipStats callRelationshipStats;
    public LocationStats callerLocationStats;
    public LocationStats calleeLocationStats;
    public Explanation.Statistics callBasedExplanationStatistics;

    public AgentPayoffWindow agentPayoffWindow = new AgentPayoffWindow();
    public AgentPayoffWindow agentCalleePayoffWindow = new AgentPayoffWindow();
    public WindowForMetric windowForDecideTimeMetric = new WindowForMetric();
    public WindowForMetric windowForFeedbackTimeMetric = new WindowForMetric();
    public WindowForMetric windowForPrivacyMetric = new WindowForMetric();
    public WindowForMetric windowForPayoffMetric = new WindowForMetric();

    private static LinkedHashMap<Long, Double> getWindowMapDouble() {
        return new LinkedHashMap<Long, Double>(){
            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, Double> eldest){
                return this.size() > windowSize;
            }
        };
    }

    private static LinkedHashMap<Long, Integer> getWindowMapInteger() {
        return new LinkedHashMap<Long, Integer>(){
            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, Integer> eldest){
                return this.size() > windowSize;
            }
        };
    }

    //cost of giving feedbacks
    public static double costFeedback = 0.5; //not used for now
    
    //cost of giving feedbacks and giving explanation;
    public static double costExplanation = 0.6; //not used for now
    
    //pay off table, if neighbor thinks callee should answer
    //public double[] payoff_a = new double[22];
    //pay off table, if neighbor thinks callee should ignore
    //during first and second simulations, neighbor thinks callee should ignore
    //during a meeting or in a library (hard-coded in this way).
    //public double[] payoff_i = new double[22];

    public PayoffCalculator payoffCalculator;
    
    //reference to all agents in the simulation
    public Bag allAgents = new Bag();
    
    //keep all calls happening in current step
    public Bag callsInThisStep = new Bag();

    public Bag allCalls = new Bag();
    
    //sum of #neighbors of all calls in a step
    //different from #neighbors involved in calls
    public int neighborCount = 0;
    //average happiness of involved people in a step
    public double overallHappiness = 0.0;
    //total payoff in a step
    public double payoff = 0.0;

    public Map<AgentType, Double> agentTypePayoffInWindow = new HashMap<>();
    public Map<AgentType, Double> agentTypeCalleePayoffInWindow = new HashMap<>();

    //Write results to file
    public BufferedWriter out = null;
    public BufferedWriter normsDataWriter = null;
    private static final String RESULTS_PATH = "results/";

    public Agents(long seed){
        super(seed);
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

    private int[] getPopulationsBasedOnRatio(int[] ratio, int total) {
        int len = ratio.length;
        int ratioSum = 0;
        for (int i = 0; i < len; i++) {
            ratioSum += ratio[i];
        }
        int[] populations = new int[len];
        int rem = total;
        double common = ((double) total) / ratioSum;
        for (int i = 0; i < len; i++) {
            if (i < (len - 1)) {
                populations[i] = (int) (common * ratio[i]);
                rem -= populations[i];
            } else {
                populations[i] = rem;
            }
        }
        return populations;
    }

    public static boolean isRuleBasedRLCase() {
        return simulationNumber == 4 || simulationNumber == 5 || simulationNumber == 7;
    }

    public static boolean isRuleBasedRLCaseListenExplanation() {
        return simulationNumber == 4 || simulationNumber == 7;
    }

    public static boolean isStateRL() {
        return simulationNumber == 6;
    }

    public static boolean isLCS() {
        return simulationNumber == 8 || simulationNumber == 9 || simulationNumber >= 10;
    }

    public static boolean isLCSListenExplanation() {
        return (simulationNumber != 15) && (simulationNumber != 18) && (simulationNumber == 8 || simulationNumber >= 10);
    }

    public static boolean isLCSNewExplanation() {
        return simulationNumber >= 16 && simulationNumber != 18;
    }

    public static boolean isLCSNewExplanationPlusOwnContext() {
        return simulationNumber >= 17 && simulationNumber != 18;
    }

    public static boolean isLCSNoExplanationOwnNorms() {
        return simulationNumber == 18;
    }

    //executed before steppings. 
    public void start(){
        super.start();
        explanationStatistics = new Explanation.Statistics();
        callBasedExplanationStatistics = new Explanation.Statistics();
        callRelationshipStats = new RelationshipStats();
        callerLocationStats = new LocationStats();
        calleeLocationStats = new LocationStats();

        //read in the payoff table.
        if (payoffScheme == 1) {
            payoffCalculator = new OGBasePayoffCalculator();
        } else if (payoffScheme == 2) {
            payoffCalculator = new UpdatedBasePayoffCalculator();
        } else {
            payoffCalculator = new MultiEmergentNormBasePayoffCalculator();
        }
        //readPayoff();

        //Debugger.debug(payoff_a, "payoff_a");
        //Debugger.debug(payoff_i, "payoff_i");

        try{
            out = new BufferedWriter(new FileWriter(RESULTS_PATH + getResultsFilePath(".csv")));
//            out = new BufferedWriter(new FileWriter("Results_Sim"+simulationNumber+".csv"));
            List<String> fields = Arrays.stream(new String[]{
                    "Step",
                    "#Calls",
                    "Payoff Per Call",
                    "Happiness Per Call",
                    "Avg Payoff in Window",
                    "Avg Happiness in Window",
            }).collect(Collectors.toList());
            for (AgentType type: AgentType.values()) {
                fields.add(String.format("Expected Payoff for %s agents", type.name().toLowerCase()));
            }
            for (AgentType type: AgentType.values()) {
                fields.add(String.format("Avg Expected Payoff for %s agents in Window", type.name().toLowerCase()));
            }
            for (AgentType type: AgentType.values()) {
                fields.add(String.format("Expected Callee Payoff for %s agents", type.name().toLowerCase()));
            }
            for (AgentType type: AgentType.values()) {
                fields.add(String.format("Avg Expected Callee Payoff for %s agents in Window", type.name().toLowerCase()));
            }
            if (MEASURE_TIME) {
                fields.add("Avg time to decide");
                fields.add("Avg time for feedbacks");
                fields.add("Avg time to decide in Window");
                fields.add("Avg time for feedbacks in Window");
            }
            fields.add("Avg privacy score");
            fields.add("Avg privacy score in Window");
            out.write(String.join(",", fields) + "\r\n");
//            out.write("Step,#Calls,Payoff Per Call,Happiness Per Call,Avg Payoff in Window,Avg Happiness in Window\r\n");
        }catch(Exception e){
            try{
                out.close();
            }catch(Exception e2){
            }
            out = null;
        }
        
        //reset everything
        window.clear();
        window2.clear();
        windowForPayoffMetric.clear();;
        windowForDecideTimeMetric.clear();
        windowForPrivacyMetric.clear();
        windowForFeedbackTimeMetric.clear();
        allAgents = new Bag();
        callsInThisStep = new Bag();
        allCalls = new Bag();
        neighborCount = 0;
        overallHappiness = 0.0;
        payoff = 0.0;
        
        //initialize all agents
        for(int i=0; i<numAgents; i++){
            Agent agent = new Agent(i);
            
            //define networks
            agent.familyCircle = (int)(i/numHomes);
            agent.colleagueCircle = i % numMeetings;
            //friend circle is random. To be updated.
            agent.friendCircle = (int)(random.nextDouble()*numParties);
            
            //Randomize call rate
            agent.callRate = random.nextGaussian()*callRate/5+callRate;
            if (isRuleBasedRLCase()) {
                agent.initializeNorms();
            }
            if (isStateRL()) {
                agent.initStateRLAlgo(simulationNumber, this);
            }
            if (isLCS()) {
                agent.initLCS(this);
            }
            allAgents.add(agent);
        }

        //Assign agent types as per the defined ratio
        int[] agentIdPermutation = getRandomPermutation(numAgents);
        int[] agentPopulationsBasedOnRatio = getPopulationsBasedOnRatio(agentSocietyRatios, numAgents);
        AgentType[] agentTypes = AgentType.values();
        System.out.println("Populations for each type of agent:");
        for (int typeId = 0; typeId < agentTypes.length; typeId++) {
            System.out.printf("%s: %d\n", agentTypes[typeId].name(), agentPopulationsBasedOnRatio[typeId]);
        }
        int currentAgentTypeId = 0;
        //Debugger.debug(agentIdPermutation, "agentIdPerm");
        int[] counts = new int[3];
        int currentSet = 0;
        for (int i = 0; i < numAgents; i++) {
            int id = agentIdPermutation[i];
            Agent agent = (Agent) allAgents.get(id);
            //Debugger.debug(i, "i", id, "id", agent.agentType, "type", currentAgentTypeId, "currentIt");
            while (currentSet >= agentPopulationsBasedOnRatio[currentAgentTypeId]) {
                currentAgentTypeId++;
                currentSet = 0;
            }
            agent.setAgentType(agentTypes[currentAgentTypeId]);
            counts[currentAgentTypeId]++;
            currentSet++;
        }
        Debugger.debug(counts, "actual counts");
        //Debugger.debug(genCount, "actual generous count");

        //Each agent keeps lists of their family, colleagues and friends
        Agent temp;
        int i = 0;
        for(i=0; i<numAgents; i++){
            Agent agent = (Agent)allAgents.get(i);
            for(int j=0; j<allAgents.size();j++){
                if (i==j) continue;
                temp = (Agent)allAgents.get(j);
                
                //keep references to members in my circles. 
                if (agent.familyCircle==temp.familyCircle)
                    agent.myFamilies.add(temp);
                else if (agent.colleagueCircle==temp.colleagueCircle)
                    agent.myColleagues.add(temp);
                else if (agent.friendCircle==temp.friendCircle)
                    agent.myFriends.add(temp);
                else
                    agent.myStrangers.add(temp);
            }
            schedule.scheduleRepeating(agent, 0, 1.0);
        }
        
        //give feedback after all calls are made
        schedule.scheduleRepeating(new Steppable(){
            public void step(SimState state){
                Agents agents = (Agents)state;
                Agent temp;
                for(int i=0;i<agents.allAgents.size();i++){
                    temp = (Agent)agents.allAgents.get(i);
                    temp.giveFeedbacks(state);
                }
            }
        }, 1, 1.0);
        
        //after each step, output information and 
        //reset isCalled and currentCall of each agent.
        schedule.scheduleRepeating(new Steppable(){
            public void step(SimState state){
                if (state.schedule.getSteps()<=1)
                    return;
                Agents agents = (Agents)state;
                Agent temp;
                
                //Sum up the number of neighbors during each call. 
                //One agent may be counted multiple times. 
                neighborCount = 0;
                RecordForLearning record;
                for(int i=0;i<agents.allAgents.size();i++){
                    temp = (Agent)agents.allAgents.get(i);
                    if (temp.isCalled){
                        
                        //if called, agent keeps a record of this call for
                        //future classification
                        record = new RecordForLearning(temp.currentCall, agents, temp);
                        if (temp.id==0)
                            System.out.println("Record ID: "+temp.data.numInstances()+"\t"+record.toCSVString());

                        //temp.records.add(record);
                        //also add to Weka dataset
                        if (simulationNumber < 4) {
                            temp.addRecord(record);
                        } else if (isRuleBasedRLCase()) {
                            //System.err.println("addRecordRL is called");
                            temp.addRecordRL(record, temp.currentCall);
                        } else if (isStateRL()) {
                            temp.addRecordStateRL(record, temp.currentCall);
                        } else if (isLCS()) {
                            temp.addRecordLCS(record, temp.currentCall);
                        }
                        
                        neighborCount += temp.currentNeighbors.size();
                        temp.isCalled = false;
                        temp.currentCall = null;
                    }
                }
                
                //Calculate the overall happiness in this step. 
                //Caller happiness: +1 if answered, -1 if ignored.
                //Neighbors: based on feedbacks. 
                //Say there are N neighbors. For each neighbor, 
                //if positive feedback, +1/N, otherwise, -1/N
                overallHappiness = 0.0;
                int peopleinvolved = 0;
                Call call;
                Feedback feedback;
                for(int i=0;i<agents.callsInThisStep.size();i++){
                    call = (Call)agents.callsInThisStep.get(i);
                    if (call.action==1){
                        overallHappiness+=1.0;
                    }
                    else{
                        overallHappiness-=1.0;
                    }
                    peopleinvolved++;
                    
                    for(int j=0;j<call.feedbacks.size();j++){
                        feedback = (Feedback)call.feedbacks.get(j);
                        if (feedback.payoff>0){
                            overallHappiness += 1.0/call.feedbacks.size();
                        }
                        else if (feedback.payoff<0){
                            overallHappiness -= 1.0/call.feedbacks.size();
                        }
                        peopleinvolved++;
                    }
                }
                
                //get average happiness per call
                //if (peopleinvolved>0)
                //    overallHappiness/=(double)peopleinvolved;
                if (agents.callsInThisStep.size()>0)
                	overallHappiness/=(double)agents.callsInThisStep.size();

                AgentPayoffs agentPayoffs = new AgentPayoffs();
                AgentPayoffs agentCalleePayoffs = new AgentPayoffs();

                //Calculate payoff
                payoff = 0.0;
                int multiplier = 1;
                double totalTimeToDecide = 0;
                double totalTimeForFeedbacks = 0;
                double totalPrivacy = 0.0;
                for(int i=0;i<agents.callsInThisStep.size();i++){
                    call = (Call)agents.callsInThisStep.get(i);
                    if (MEASURE_TIME) {
                        totalTimeToDecide += call.timeToDecide;
                        totalTimeForFeedbacks += call.timeToGiveFeedbacks;
                    }
                    totalPrivacy += call.privacy;
                    //Answer call:
                    if (call.action==1){
                        //Callee payoff
                        double calleePayoff =
                                payoffCalculator.calculateCalleePayoff(Context.builder()
                                        .callUrgency(call.urgency)
                                        .callerRelationship(Agent.getCallerRelationship(call))
                                        .build(), Action.fromID(call.action));
//                        if (call.isStranger())
//                            calleePayoff = (call.urgency?payoff_a[6]:payoff_a[4]);
//                        else
//                            calleePayoff =(call.urgency?payoff_a[2]:payoff_a[0]);

                        payoff+= calleePayoff;

                        //Collecting per agent type callee payoff
                        agentPayoffs.add(call.callee, calleePayoff);
                        agentCalleePayoffs.add(call.callee, calleePayoff);

                        //Caller payoff
                        double callerPayoff =
                                payoffCalculator.calculateCallerPayoff(Context.builder()
                                        .callUrgency(call.urgency).build(), Action.fromID(call.action));
                        //double callerPayoff = (call.urgency?payoff_a[10]:payoff_a[8]);
                        agentPayoffs.add(call.caller, callerPayoff);
                        payoff += callerPayoff;
                        
                        //In first and second simulations, 
                        //Neighbor thinks callee should ignore calls during a meeting, 
                        //in a library, or at a party.
                        /*
                        int l = (int)(call.location/Agents.numAgents);
                        switch(l){
                            case 1: 
                                payoff += multiplier*payoff_i[12+2*l];
                                break;
                            case 2: 
                                payoff += multiplier*payoff_i[12+2*l];
                                break;
                            case 3:
                                payoff += multiplier*payoff_i[12+2*l];
                                break;
                            default:
                                payoff += multiplier*payoff_a[12+2*l];
                                break;
                        }*/
                    }
                    //Ignore call:
                    else{
                        //Callee payoff
                        double calleePayoff =
                                payoffCalculator.calculateCalleePayoff(Context.builder()
                                        .callUrgency(call.urgency)
                                        .callerRelationship(Agent.getCallerRelationship(call))
                                        .build(), Action.fromID(call.action));
//                        if (call.isStranger())
//                            calleePayoff =(call.urgency?payoff_a[7]:payoff_a[5]);
//                        else
//                            calleePayoff =(call.urgency?payoff_a[3]:payoff_a[1]);

                        payoff += calleePayoff;
                        agentPayoffs.add(call.callee, calleePayoff);
                        agentCalleePayoffs.add(call.callee, calleePayoff);
                        //Caller payoff
                        //double callerPayoff = (call.urgency?payoff_a[11]:payoff_a[9]);
                        double callerPayoff =
                                payoffCalculator.calculateCallerPayoff(Context.builder()
                                        .callUrgency(call.urgency).build(), Action.fromID(call.action));
                        agentPayoffs.add(call.caller, callerPayoff);
                        payoff += callerPayoff;
                        
                        //In first and second simulations, 
                        //Neighbor thinks callee should ignore calls during a meeting, 
                        //in a library, or at a party.
                        /*
                        int l = (int)(call.location/Agents.numAgents);
                        switch(l){
                            case 1: 
                                payoff += multiplier*payoff_i[13+2*l];
                                break;
                            case 2: 
                                payoff += multiplier*payoff_i[13+2*l];
                                break;
                            case 3:
                                payoff += multiplier*payoff_i[13+2*l];
                                break;
                            default:
                                payoff += multiplier*payoff_a[13+2*l];
                                break;
                        }*/
                    }

                    //UPDATE: Use the actual payoffs the neighbors gave
                    multiplier = call.feedbacks.size();
                    double n = 0.0;
                    for(int j=0;j<call.feedbacks.size();j++){
                        Feedback callFeedback = (Feedback) call.feedbacks.get(j);
                        agentPayoffs.add(callFeedback.giver, callFeedback.payoff / multiplier);
                        n += callFeedback.payoff;
                    }
                    if (multiplier>0)
                    	n /= multiplier;
                    //n is the average neighbor payoff
                    if (n >= 0) {
                        callBasedExplanationStatistics.addAccept(Action.fromID(call.action),
                                Location.get(call.location / Agents.numAgents));
                    } else {
                        callBasedExplanationStatistics.addReject(Action.fromID(call.action),
                                Location.get(call.location / Agents.numAgents));
                    }
                    payoff += n;
                }
                
                //When calculating overallHappiness, we don't consider callees.
                //When calculating payoffs, callees are also involved. 
                //peopleinvolved += agents.callsInThisStep.size();
                //get average payoff of all involved people
                //if (peopleinvolved>0)
                //    payoff/=(double)peopleinvolved;
                //UPDATE: average payoff per call
                if (agents.callsInThisStep.size()>0)
                	payoff /= (double)agents.callsInThisStep.size();
                

                if (MEASURE_TIME && agents.callsInThisStep.size() > 0) {
                    totalTimeToDecide /= agents.callsInThisStep.size();
                    totalTimeForFeedbacks /= agents.callsInThisStep.size();
                    windowForDecideTimeMetric.add(state.schedule.getSteps(), totalTimeToDecide);
                    windowForFeedbackTimeMetric.add(state.schedule.getSteps(), totalTimeForFeedbacks);
                }
                if (agents.callsInThisStep.size() > 0) {
                    totalPrivacy /= agents.callsInThisStep.size();
                }
                windowForPrivacyMetric.add(state.schedule.getSteps(), totalPrivacy);
                //System.out.println("Step: "+state.schedule.getSteps()+"\t#calls: "+agents.callsInThisStep.size());
                //System.out.println("Step: "+state.schedule.getSteps()+"\t#neighbors: "+neighborCount);
                //System.out.println();

                if (out!=null){
                    try{
                        //out.write(""+state.schedule.getSteps()+","+agents.callsInThisStep.size()+","+payoff+","+overallHappiness);
                        String[] row = new String[]{
                                Long.toString(state.schedule.getSteps()),
                                Integer.toString(agents.callsInThisStep.size()),
                                Double.toString(payoff),
                                Double.toString(overallHappiness)
                        };
//                        List<String> rowList = Arrays.stream(row).collect(Collectors.toList());
//                        for (AgentType type: AgentType.values()) {
//                            rowList.add(Double.toString(agentPayoffs.payoffs.getOrDefault(type, 0.0)));
//                        }
                        out.write(String.join(",", row));
                    }catch(Exception e){
                        try{out.close();}catch(Exception e2){}
                        out = null;
                    }
                }
                //System.out.print(""+state.schedule.getSteps()+","+payoff+","+overallHappiness);

                agentPayoffWindow.add(state.schedule.getSteps(), agentPayoffs);
                agentCalleePayoffWindow.add(state.schedule.getSteps(), agentCalleePayoffs);

                //Output the average payoff over a window
                window.put(state.schedule.getSteps(), payoff);
                windowForPayoffMetric.add(state.schedule.getSteps(), payoff);
                numCalls.put(state.schedule.getSteps(), agents.callsInThisStep.size());
                
                //Use mean of window, considering #calls in each step
                payoff = 0.0;
                int callcount = 0;
                int totalcallcount = 0;
                for(Map.Entry<Long, Double> entry : window.entrySet()){
                	callcount = numCalls.get(entry.getKey());
                    payoff+=entry.getValue()*callcount;
                    totalcallcount+=callcount;
                }
                if (totalcallcount>0)
                //payoff /= window.size();
                	payoff /= totalcallcount;
                
                //Use average over window2 as output
                window2.put(state.schedule.getSteps(), overallHappiness);
                //Use mean of window, considering #calls in each step
                overallHappiness = 0.0;
                for(Map.Entry<Long, Double> entry : window2.entrySet()){
                	callcount = numCalls.get(entry.getKey());
                    overallHappiness+=entry.getValue()*callcount;
                }
                //overallHappiness /= window2.size();
                if (totalcallcount>0)
                	overallHappiness /= totalcallcount;
                
                //Use median of window
                /*
                Double x[] = window.values().toArray(new Double[window.size()]);
                Arrays.sort(x);
                payoff = x[(int)(x.length/2)];*/
                
                //System.out.println("Step: "+state.schedule.getSteps()+"\tOverall Happiness: "+overallHappiness);
                //System.out.println("Step: "+state.schedule.getSteps()+"\tPayoff: "+payoff);
                //System.out.println(","+payoff+","+overallHappiness);
                if (out!=null){
                    try{
                        //out.write(","+payoff+","+overallHappiness+"\r\n");
                        out.write(","+payoff+","+overallHappiness);
                        List<String> agentPayoffTokens = new ArrayList<>();
                        for (AgentType type : AgentType.values()) {
                            agentPayoffTokens.add(Double.toString(agentPayoffs.getAvgPayoff(type)));
                        }
                        agentTypePayoffInWindow.clear();
                        for (AgentType type : AgentType.values()) {
                            double agentPayoffInWindow = agentPayoffWindow.getAvgPayoff(type);
                            agentTypePayoffInWindow.put(type, agentPayoffInWindow);
                            agentPayoffTokens.add(Double.toString(agentPayoffInWindow));
                        }
                        for (AgentType type : AgentType.values()) {
                            agentPayoffTokens.add(Double.toString(agentCalleePayoffs.getAvgPayoff(type)));
                        }
                        agentTypeCalleePayoffInWindow.clear();
                        for (AgentType type : AgentType.values()) {
                            double agentCalleePayoffInWindow = agentCalleePayoffWindow.getAvgPayoff(type);
                            agentTypeCalleePayoffInWindow.put(type, agentCalleePayoffInWindow);
                            agentPayoffTokens.add(Double.toString(agentCalleePayoffInWindow));
                        }
                        out.write("," + String.join(",", agentPayoffTokens));
                        if (MEASURE_TIME) {
                            out.write("," + totalTimeToDecide + "," + totalTimeForFeedbacks);
                            out.write("," + windowForDecideTimeMetric.getAvgMetric() + "," +
                                    windowForFeedbackTimeMetric.getAvgMetric());
                        }
                        out.write("," + totalPrivacy);
                        out.write("," + windowForPrivacyMetric.getAvgMetric());
                        out.write("\r\n");
                    }catch(Exception e){
                        try{out.close();}catch(Exception e2){}
                        out = null;
                    }
                }
                
                callsInThisStep = new Bag();
            }
        }, 2, 1.0);
        System.out.println("Simulation started.");
    }

    private String getResultsFilePath(String extension) {
        StringBuilder builder = new StringBuilder();
        builder.append("Results_Sim");
        builder.append(simulationNumber);
        for (int rat: agentSocietyRatios) {
            builder.append("_").append(rat);
        }
        if (payoffCalculator instanceof UpdatedBasePayoffCalculator) {
            builder.append("_updated_base");
        } else if (payoffCalculator instanceof MultiEmergentNormBasePayoffCalculator) {
            builder.append("_multi_updated_base");
        }

        if (trial != 0) {
            builder.append("_trial").append(trial);
        }
        if (extension != null) {
            builder.append(extension);
        }
        return builder.toString();
    }

    private String getResultsFilePath() {
        return getResultsFilePath(null);
    }

    private String getEmergentNormsPath() {
        String filePath = getResultsFilePath();
        return filePath + "_emergent_norms.txt";
    }

    private String getDetailedNormsDataPath() {
        String filePath = getResultsFilePath();
        return filePath + "_norms_data.csv";
    }

    //Get all agents currently in a location
    public Bag getNeighbors(int location){
        Bag neighbors = new Bag();
        Agent temp;
        for(int i=0;i<allAgents.size();i++){
            temp = (Agent)allAgents.get(i);
            if (temp.location==location)
                neighbors.add(temp);
        }
        return neighbors;
    }
    
//    public void readPayoff(){
//        payoff_a = new double[]{1,0,2,-1,0,0.5,1,-0.5,1,-1,2,-2,0,0,1,-1,0,0,1,-1,0,0};
//        payoff_i = new double[]{1,0,2,-1,0,0.5,1,-0.5,1,-1,2,-2,0,0,-1,1,0,0,-1,1,0,0};
//        try{
//            BufferedReader reader = new BufferedReader(new FileReader("payoff.txt"));
//            String line;
//            String[] items;
//            int i = 0;
//            while((line=reader.readLine())!=null){
//                if (i>=payoff_a.length) break;
//                line = line.trim();
//                if (line.length()<=0) continue;
//                if (line.startsWith("#")) continue;
//                items = line.split("\\s+");
//                payoff_a[i] = Double.parseDouble(items[0]);
//                try{
//                    payoff_i[i] = Double.parseDouble(items[1]);
//                }catch(Exception ee){
//                    payoff_i[i] = payoff_a[i];
//                }
//                i++;
//            }
//            reader.close();
//        }catch(Exception e){
//            e.printStackTrace();
//        }
//    }

    private void getNorms() {
        Debugger.debug("Entered norms", "status");
//        InstanceGenerator generator = new CallsBasedInstanceGenerator(allCalls);
//        Instances instances = generator.getInstances();
//        BulkClassifier bulkClassifier = new VotingBasedBulkClassifier();
//        Instances labeledInstances = bulkClassifier.classifyAll(instances);
        VotingBasedCallClassifier votingBasedCallClassifier = new VotingBasedCallClassifier(this, allAgents);
        Debugger.debug("Intialized voting based call classifier", "status");
        Debugger.debug(allCalls.size(), "allCalls.size()");
        Instances labeledInstances = votingBasedCallClassifier.getLabeledInstances(allCalls);
        Debugger.debug("Got labeled instances", "status");
        J48 decisionTree = new J48();
        decisionTree.setUnpruned(true);
        try {
            decisionTree.buildClassifier(labeledInstances);
            System.out.println(decisionTree.toString());
            System.out.println(decisionTree.graph());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<NormDataRow> getNormsData() {
        Map<Norm, Double> normVoteMap = new LinkedHashMap<>();
        for (int i = 0; i < numAgents; i++) {
            Agent agent = (Agent) allAgents.get(i);
            agent.contributeNorms(normVoteMap);
        }
        List<NormDataRow> normsData = new ArrayList<>();
        for (Map.Entry<Norm, Double> entry : normVoteMap.entrySet()) {
            Norm norm = entry.getKey();
            double support = getSupport(norm);
            NormDataRow dataRow = new NormDataRow(norm, support);
            if (support > NORM_EMERGENCE_THRESHOLD) {
                dataRow.isEmerged = true;
            }
            normsData.add(dataRow);
        }

        int dataSize = normsData.size();

        for (int i = 0; i < dataSize; i++) {
            NormDataRow dataRowI = normsData.get(i);
            for (int j = 0; j < dataSize; j++) {
                if (i == j) continue;
                NormDataRow dataRowJ = normsData.get(j);
                if (dataRowJ.isEmerged && dataRowJ.norm.subsumes(dataRowI.norm)) {
                    dataRowI.isSubsumedByEmerged = true;
                }
            }
        }

        return normsData;
    }

    private List<ElectionResult> getEmergentNormsLCS(List<NormDataRow> normsData) {
        List<ElectionResult> popularNorms = new ArrayList<>();
        List<ElectionResult> allNorms = new ArrayList<>();
        List<ElectionResult> emergedNorms = new ArrayList<>();

        for (NormDataRow dataRow : normsData) {
            allNorms.add(new ElectionResult(dataRow.norm, dataRow.acceptanceRate));
            if (dataRow.isEmerged) {
                popularNorms.add(new ElectionResult(dataRow.norm, dataRow.acceptanceRate));
                if (!dataRow.isSubsumedByEmerged) {
                    emergedNorms.add(new ElectionResult(dataRow.norm, dataRow.acceptanceRate));
                }
            }
        }

        allNorms.sort(Comparator.comparing(ElectionResult::getPopularityRatio).reversed());
//
//        try {
//            out.write("\n");
//            out.write("== All norm votes ==\n");
//            for (ElectionResult electionResult : allNorms) {
//                out.write(String.format("[%s, popularity: %f]\n", electionResult.norm, electionResult.popularityRatio));
//            }
//            out.write("\n");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        System.out.println();
        System.out.println("== All norm votes ==");
        for (ElectionResult electionResult : allNorms) {
            System.out.printf("[%s, popularity: %f]\n", electionResult.norm, electionResult.popularityRatio);
        }

        System.out.println();
        System.out.println("== All popular rules ===");
        for (ElectionResult electionResult : popularNorms) {
            System.out.printf("[%s, popularity: %f]\n", electionResult.norm, electionResult.popularityRatio);
        }

        return emergedNorms.stream()
                .sorted(Comparator.comparing(ElectionResult::getPopularityRatio).reversed())
                .collect(Collectors.toList());
    }

    private List<ElectionResult> getEmergentNormsLCS() {
        Map<Norm, Double> normVoteMap = new LinkedHashMap<>();
        for (int i = 0; i < numAgents; i++) {
            Agent agent = (Agent) allAgents.get(i);
            agent.contributeNorms(normVoteMap);
        }
        List<ElectionResult> popularNorms = new ArrayList<>();
        List<ElectionResult> allNorms = new ArrayList<>();
        for (Map.Entry<Norm, Double> entry : normVoteMap.entrySet()) {
            Norm norm = entry.getKey();
            double support = getSupport(norm);
//            System.out.printf("[%s, popularity: %f]\n", norm, support);
            allNorms.add(new ElectionResult(norm, support));
            if (getSupport(entry.getKey()) >= NORM_EMERGENCE_THRESHOLD) {
                popularNorms.add(new ElectionResult(norm, support));
            }
        }

        allNorms.sort(Comparator.comparing(ElectionResult::getPopularityRatio).reversed());

        System.out.println();
        System.out.println("== All norm votes ==");
        for (ElectionResult electionResult : allNorms) {
            System.out.printf("[%s, popularity: %f]\n", electionResult.norm, electionResult.popularityRatio);
        }

        RuleSet ruleSet = new RuleSet(popularNorms.stream().map(er -> new Rule(er.norm)).collect(Collectors.toList()));

        System.out.println();
        System.out.println("== All popular rules ===");
        ruleSet.print();
        ruleSet.subsumeAll();
        popularNorms = popularNorms.stream().filter(er -> ruleSet.contains(new Rule(er.norm)))
                .sorted(Comparator.comparing(ElectionResult::getPopularityRatio).reversed())
                .collect(Collectors.toList());
//        popularNorms.sort(Comparator.comparing(ElectionResult::getPopularityRatio).reversed());
        return popularNorms;
    }

    private static class NormDataRow {
        public Norm norm;
        public double acceptanceRate;
        public boolean isEmerged;

        //Only relevant if isEmerged is true
        public boolean isSubsumedByEmerged;

        public NormDataRow(Norm norm, double acceptanceRate) {
            this.norm = norm;
            this.acceptanceRate = acceptanceRate;
            this.isEmerged = false;
            this.isSubsumedByEmerged = false;
        }

        public String toCSV() {
            return String.join(",", norm.toString(), Double.toString(acceptanceRate),
                    Boolean.toString(isEmerged), Boolean.toString(isSubsumedByEmerged));
        }
    }

    private static class ElectionResult {
        public Norm norm;
        public double popularityRatio;

        public ElectionResult(Norm norm, double popularityRatio) {
            this.norm = norm;
            this.popularityRatio = popularityRatio;
        }

        public double getPopularityRatio() {
            return popularityRatio;
        }
    }

    private double getSupport(Norm norm) {
        int votes = 0;
        for (int i = 0; i < numAgents; i++) {
            Agent agent = (Agent) allAgents.get(i);
            if (agent.lcsAlgorithm.getAcceptableDecisions(Collections.singletonList(norm), false)
                    .contains(norm.consequent)) {
                votes++;
            }
        }
        return ((double)votes) / numAgents;
    }

    private String getNormsDataHeader() {
        List<String> fields = Arrays.stream(new String[]{
                "norm",
                "acceptance_rate",
                "is_emerged",
                "is_subsumed_by_emerged"
        }).collect(Collectors.toList());
        return String.join(",", fields);
    }

    public void finish(){
        super.finish();
        try{out.close();}catch(Exception e){}
        out = null;
        if (isRuleBasedRLCase()) {
            Agent agent = (Agent) allAgents.get(0);
            List<NormEntry> tempEntries = agent.internalNorms.stream()
                    .sorted(Comparator.comparing((NormEntry en) -> en.weight).reversed()).collect(Collectors.toList());
            for (NormEntry normEntry : tempEntries) {
                System.out.printf("[%s, %s]\n", normEntry.norm, normEntry.weight);
            }
        } else if (isLCS()) {
            Agent agent = (Agent) allAgents.get(0);
            agent.lcsAlgorithm.printStats();
        }
        try {
            out = new BufferedWriter(new FileWriter(RESULTS_PATH + getEmergentNormsPath()));
            normsDataWriter = new BufferedWriter(new FileWriter(RESULTS_PATH + getDetailedNormsDataPath()));
            normsDataWriter.write(getNormsDataHeader() + "\n");
            out.write(explanationStatistics.toString() + "\n");
            System.out.println(explanationStatistics.toString());
            out.write("========== Call Based explanation stats ===============\n");
            System.out.println("========== Call Based explanation stats ===============");
            out.write(callBasedExplanationStatistics.toString() + "\n");
            System.out.println(callBasedExplanationStatistics.toString());
            out.write("========== Call Based explanation stats ends===============\n");
            System.out.println("========== Call Based explanation stats ends===============");
            out.write("== Callee Location Stats==\n");
            System.out.println("== Callee Location Stats==");
            out.write(calleeLocationStats.toString() + "\n");
            System.out.println(calleeLocationStats.toString());
            out.write("== Caller Location Stats==\n");
            System.out.println("== Caller Location Stats==");
            out.write(callerLocationStats.toString() + "\n");
            System.out.println(callerLocationStats.toString());
            out.write(callRelationshipStats.toString() + "\n");
            System.out.println(callRelationshipStats.toString());
            System.out.printf("Last ignore step: %d\n", lastIgnoreStep);
            System.out.printf("Last exception case step: %d\n", lastExceptionCaseStep);
            if (MEASURE_TIME) {
                System.out.printf("Avg time to decide : %f\n", windowForDecideTimeMetric.getAvgMetric());
                System.out.printf("Avg time for feedbacks: %f\n", windowForFeedbackTimeMetric.getAvgMetric());
            }
            System.out.printf("Avg privacy score: %f\n", windowForPrivacyMetric.getAvgMetric());
            out.write(String.format("Avg privacy score: %f\n", windowForPrivacyMetric.getAvgMetric()));
            System.out.printf("Avg payoff: %f\n", windowForPayoffMetric.getAvgMetric());
            out.write(String.format("Avg payoff: %f\n", windowForPayoffMetric.getAvgMetric()));
            //getNorms();
            if (isLCS()) {
                List<NormDataRow> normsData = getNormsData();
                for (NormDataRow normDataRow : normsData) {
                    normsDataWriter.write(normDataRow.toCSV() + "\n");
                }
                System.out.println("========== Emergent Norms ===============");
//                out.write("========== Emergent Norms ===============\n");
                List<ElectionResult> electionResults = getEmergentNormsLCS(normsData);
                System.out.println();
                System.out.println("=== Final Emergent Norms ===");
                out.write("=== Final Emergent Norms ===\n");
                for (ElectionResult result : electionResults) {
                    System.out.printf("[%s, popularity: %f]\n", result.norm, result.popularityRatio);
                    out.write(String.format("[%s, popularity: %f]\n", result.norm, result.popularityRatio));
//                out.printf("[%s, popularity: %f]\n", result.norm, result.popularityRatio);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try{out.close();}catch(Exception e){}
        try {
            normsDataWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("I am finished");
    }
    
    public static void main(String[] args){
        doLoop(Agents.class, args);
        System.exit(0);
    }

    private static class WindowForMetric {
        private LinkedHashMap<Long, Double> metricMap;

        public WindowForMetric() {
            this.metricMap = getWindowMapDouble();
        }

        public void add(long stepId, double metric) {
            metricMap.put(stepId, metric);
        }

        public double getAvgMetric() {
            int total = 0;
            double metric = 0.0;
            for (Map.Entry<Long, Double> entry : metricMap.entrySet()) {
                metric += entry.getValue();
                total++;
            }
            if (total > 0) {
                return metric / total;
            } else {
                return 0.0;
            }
        }

        public void clear() {
            this.metricMap.clear();
        }
    }

    private static class AgentPayoffWindow {
        private Map<AgentType, LinkedHashMap<Long, Double>> payoffWindow;
        private Map<AgentType, LinkedHashMap<Long, Integer>> countWindow;

        public AgentPayoffWindow() {
            this.payoffWindow = new HashMap<>();
            for (AgentType type : AgentType.values()) {
                payoffWindow.put(type, getWindowMapDouble());
            }
            this.countWindow = new HashMap<>();
            for (AgentType type : AgentType.values()) {
                countWindow.put(type, getWindowMapInteger());
            }
        }

        public void add(long stepId, AgentPayoffs agentPayoffs) {
            for (AgentType type : AgentType.values()) {
                double payoff = agentPayoffs.getAvgPayoff(type);
                int count = agentPayoffs.getCount(type);
                LinkedHashMap<Long, Double> typePayoffWindow = payoffWindow.get(type);
                typePayoffWindow.put(stepId, payoff);
                LinkedHashMap<Long, Integer> typeCountWindow = countWindow.get(type);
                typeCountWindow.put(stepId, count);
                payoffWindow.put(type, typePayoffWindow);
                countWindow.put(type, typeCountWindow);
            }
        }

        public double getAvgPayoff(AgentType type) {
//            boolean debug = type == AgentType.GENEROUS;
            LinkedHashMap<Long, Double> typePayoffWindow = payoffWindow.get(type);
            LinkedHashMap<Long, Integer> typeCountWindow = countWindow.get(type);
            int total = 0;
            double payoffs = 0.0;
            for (Map.Entry<Long, Double> entry : typePayoffWindow.entrySet()) {
                int count = typeCountWindow.get(entry.getKey());
                payoffs += entry.getValue() * count;
                total += count;
            }
//            if (debug) {
//                //Debugger.debug("window get avg", "status", payoffs, "payoffs", total, "total");
//            }
            if (total > 0) {
                return payoffs / total;
            } else {
                return 0.0;
            }
        }
    }

    public static class LocationStats {
        private Map<Location, Integer> stats;
        private int total;

        public LocationStats() {
            this.stats = new HashMap<>();
            this.total = 0;
        }

        public void add(Location location, int count) {
            int value = stats.getOrDefault(location, 0);
            stats.put(location, value + count);
            total += count;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("======== Location Stats ============").append("\n");
            for (Location location : Location.values()) {
                int stat = this.stats.getOrDefault(location, 0);
                builder.append(String.format("%s:== [total: %d, stat: %d, ratio (stat/total): %f]", location.name(),
                                total, stat,
                                ((double)stat) / total))
                        .append("\n");
            }
            builder.append("========== Location Stats end ==============").append("\n");
            return builder.toString();
        }
    }

    public static class RelationshipStats {
        private Map<Relationship, Integer> stats;
        private int total;

        public RelationshipStats() {
            this.stats = new HashMap<>();
            this.total = 0;
        }

        public void add(Relationship relationship, int count) {
            int value = stats.getOrDefault(relationship, 0);
            stats.put(relationship, value + count);
            total += count;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("======== Relationship Stats ============").append("\n");
            for (Relationship relationship : Relationship.values()) {
                int stat = this.stats.getOrDefault(relationship, 0);
                builder.append(String.format("%s:== [total: %d, stat: %d, ratio (stat/total): %f]", relationship.name(),
                                                                                                            total, stat,
                                                                                                ((double)stat) / total))
                      .append("\n");
            }
            builder.append("========== Relationship Stats end ==============").append("\n");
            return builder.toString();
        }
    }

    private static class AgentPayoffs {
        private Map<AgentType, Double> payoffs;
        private Map<AgentType, Integer> counts;

        public AgentPayoffs() {
            this.payoffs = new HashMap<>();
            this.counts = new HashMap<>();
        }

        public void add(Agent agent, double payoff) {
//            boolean debug = agent.agentType == AgentType.GENEROUS;
//            if (debug) {
//                Debugger.debug("add agent payoff", "status", agent.agentType, "type", payoff, "payoff");
//            }
            this.payoffs.put(agent.agentType, payoffs.getOrDefault(agent.agentType, 0.0) + payoff);
            this.counts.put(agent.agentType, counts.getOrDefault(agent.agentType, 0) + 1);
        }

        /**
         * Total payoff for people of this type divided by the number of instances where payoff is calculated
         */
        public double getAvgPayoff(AgentType agentType) {
            int count = counts.getOrDefault(agentType, 0);
            if (count == 0) {
                return 0.0;
            } else {
                return payoffs.getOrDefault(agentType, 0.0) / count;
            }
        }

        public int getCount(AgentType type) {
            return counts.getOrDefault(type, 0);
        }
    }
}