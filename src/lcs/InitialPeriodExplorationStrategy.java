package lcs;

public class InitialPeriodExplorationStrategy implements ExplorationStrategy {
    private int steps;
    private int maxSteps;
    private ExplorationStrategy explorationStrategy;

    public InitialPeriodExplorationStrategy(ExplorationStrategy explorationStrategy, int maxSteps) {
        this.steps = 0;
        this.maxSteps = maxSteps;
        this.explorationStrategy = explorationStrategy;
    }

    @Override
    public boolean explore() {
        steps++;
        if (steps <= maxSteps) {
            return explorationStrategy.explore();
        }
        return false;
    }
}
