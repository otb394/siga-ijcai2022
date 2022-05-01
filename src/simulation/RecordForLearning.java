package simulation;

import base.Action;
import base.Context;

/**
 *
 * @author Hui
 */
public class RecordForLearning {
    
    //Weights of callee, caller and neighbor payoffs
    //PAYOFF = AVG(weights[0]*Callee_Payoff, weights[1]*Caller_Payoff, 
    //  weights[2]*AVG(Neighbor_Payoff))
	//Perfect
    //public static double[] weights = new double[]{1.0,1.0,1.0};
    //Selfish
    //public static double[] weights = new double[]{3.0,0.0,0.0};
    //Generous
    //public static double[] weights = new double[]{1.5,0.0,1.5};
    
    public static String[] relationTypes = new String[]{"family","colleague","friend","stranger"};
    
    public int location;
    public int callerRelation;
    public boolean urgency;
    public int action; //0 for ignored, 1 for answered
    public boolean existsFamily;
    public boolean existsColleague;
    public boolean existsFriend;
    public double calleePayoff;
    public double callerPayoff;
    public double averageNeighborPayoff;
    public Agent agent;
    
    public RecordForLearning(){
        location = 0;
        callerRelation = 3;
        urgency = false;
        action = 0;
        existsFamily = false;
        existsColleague = false;
        existsFriend = false;
        calleePayoff = 0.0;
        callerPayoff = 0.0;
        averageNeighborPayoff = 0.0;
    }
    
    public RecordForLearning(Call call, Agents agents, Agent agent){
        this.agent = agent;
        this.location = (int)(call.location/Agents.numAgents);
        if (call.isFamily())
            this.callerRelation = 0;
        else if (call.isColleague())
            this.callerRelation = 1;
        else if (call.isFriend())
            this.callerRelation = 2;
        else
            this.callerRelation = 3;
        
        this.urgency = call.urgency;
        this.action = call.action;
        
        this.existsFamily = false;
        this.existsColleague = false;
        this.existsFriend = false;

        //callee payoff and caller payoff
        this.calleePayoff = 0.0;
        this.callerPayoff = 0.0;
        callerPayoff = agents.payoffCalculator.calculateCallerPayoff(
                Context.builder().callUrgency(call.urgency).build(), Action.fromID(call.action));
        calleePayoff = agents.payoffCalculator.calculateCalleePayoff(
                Context.builder().callUrgency(call.urgency)
                                 .callerRelationship(Agent.getCallerRelationship(call))
                                 .build(),
                Action.fromID(call.action));
//        if (call.action==1){
//            //Callee payoff
//            //if Callee does not know caller
//            if (call.isStranger()){
//                calleePayoff = (call.urgency?agents.payoff_a[6]:agents.payoff_a[4]);
//            }else
//                calleePayoff = (call.urgency?agents.payoff_a[2]:agents.payoff_a[0]);
//            //Caller payoff
//            //callerPayoff = (call.urgency?agents.payoff_a[10]:agents.payoff_a[8]);
//        }else{
//            //Callee payoff
//            if (call.isStranger()){
//                calleePayoff = (call.urgency?agents.payoff_a[7]:agents.payoff_a[5]);
//            }else
//                calleePayoff = (call.urgency?agents.payoff_a[3]:agents.payoff_a[1]);
//            //Caller payoff
//            //callerPayoff = (call.urgency?agents.payoff_a[11]:agents.payoff_a[9]);
//        }
        
        averageNeighborPayoff = 0.0;
        Feedback feedback;
        for(int i=0;i<call.feedbacks.size();i++){
            feedback = (Feedback)call.feedbacks.get(i);
            if (feedback.giver.familyCircle==call.callee.familyCircle)
                this.existsFamily = true;
            if (feedback.giver.colleagueCircle==call.callee.colleagueCircle)
                this.existsColleague = true;
            if (feedback.giver.friendCircle==call.callee.friendCircle)
                this.existsFriend = true;
            
            averageNeighborPayoff+= feedback.payoff;
        }
        if (call.feedbacks.size()>0)
            averageNeighborPayoff/=call.feedbacks.size();
    }
    
    public double getPayoff(){
        return (agent.agentType.weights[0]*calleePayoff
                +agent.agentType.weights[1]*callerPayoff
                +agent.agentType.weights[2]*averageNeighborPayoff)/3.0;
    }
    
    public int getOrdinalFeedback(){
        double payoff = this.getPayoff();
        if (payoff>=1) return 2;
        else if (payoff>=0.5)
            return 1;
        else if (payoff>=-0.5)
            return 0;
        else if (payoff>=-1)
            return -1;
        else
            return -2;
    }
    
    public String toCSVString(){
        return Agents.locations[location]+","
                +relationTypes[callerRelation]+","
                +urgency+","
                +existsFamily+","
                +existsColleague+","
                +existsFriend+","
                +(action==1?"Answer":"Ignore")+","
                +calleePayoff+","
                +callerPayoff+","
                +averageNeighborPayoff;
    }
    
}
