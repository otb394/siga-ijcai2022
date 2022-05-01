package simulation;

public enum AgentType {
    PERFECT(new double[]{1.0, 1.0, 1.0}),
    SELFISH(new double[]{3.0, 0.0, 0.0}),
    GENEROUS(new double[]{1.5, 0.0, 1.5});

    public final double[] weights;

    AgentType(double[] weights) {
        this.weights = weights;
    }
}
