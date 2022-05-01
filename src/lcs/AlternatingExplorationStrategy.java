package lcs;

public class AlternatingExplorationStrategy implements ExplorationStrategy {
    private Mode mode;

    public AlternatingExplorationStrategy() {
        this.mode = Mode.EXPLORE;
    }

    @Override
    public boolean explore() {
        if (mode == Mode.EXPLORE) {
            mode = Mode.EXPLOIT;
            return true;
        } else {
            mode = Mode.EXPLORE;
            return false;
        }
    }

    private enum Mode {
        EXPLOIT, EXPLORE
    }
}
