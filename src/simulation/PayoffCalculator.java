package simulation;

import base.Action;
import base.Context;

public abstract class PayoffCalculator {

    //public abstract Payoffs calculate(Context context, base.Action calleeAction, boolean neighborAccept);

    public abstract double calculateNeighborPayoff(Context context, Action calleeAction, boolean neighborAccept);
    public abstract double calculateCalleePayoff(Context context, Action calleeAction);
    public abstract double calculateCallerPayoff(Context context, Action calleeAction);


    public static class Payoffs {
        public double calleePayoff;
        public double callerPayoff;
        public double neighborPayoff;
    }
}
