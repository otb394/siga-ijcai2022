package base;

public enum Action {
    RING(1), IGNORE(0);

    public int id;

    Action(int id) {
        this.id = id;
    }

    public static Action fromID(int id) {
        if (id == 1) return RING;
        return IGNORE;
    }
}
