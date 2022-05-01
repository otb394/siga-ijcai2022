package simulation;

/**
 *
 * @author Hui
 */
public class Feedback {
    public Call call;
    public Agent giver;
    public double payoff;//instead of boolean feedback
    
    public Feedback(Call call, Agent giver, double payoff){
        this.call = call;
        this.giver = giver;
        this.payoff = payoff;
    }
}
