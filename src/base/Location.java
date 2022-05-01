package base;

public enum Location {
    HOME, MEETING, PARTY, LIBRARY, ER, OFFICE, THEATER, CLASSROOM, CAMPUS, CONCERT, RESTAURANT;

    public static Location get(int id) {
        switch (id) {
            case 0: return HOME;
            case 1: return MEETING;
            case 2: return PARTY;
            case 3: return LIBRARY;
            case 4:
            default: return ER;
        }
    }
}
