package lcs;

import util.Randomizer;

public class EpsilonGreedyExplorationStrategy implements ExplorationStrategy {
    private Randomizer randomizer;
    private double epsilon;

    public EpsilonGreedyExplorationStrategy(Randomizer randomizer, double epsilon) {
        this.randomizer = randomizer;
        this.epsilon = epsilon;
    }

    public EpsilonGreedyExplorationStrategy(Randomizer randomizer) {
        this.randomizer = randomizer;
        this.epsilon = 0.1;
    }

    @Override
    public boolean explore() {
        return randomizer.random.nextBoolean(epsilon);
    }
}
