package simulation;

import base.Action;
import base.Context;
import base.Location;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class Explanation {
    public abstract boolean accept(Agent agent, Action calleeAction, Context neighborContext);

    public static class StatisticsUtil {
        public int noOfAccepts;
        public int noOfRejects;
        public int ringAccepts;
        public int ignoreAccepts;
        public int ringRejects;
        public int ignoreRejects;

        public void StatisticsUtil() {
            this.noOfAccepts = 0;
            this.noOfRejects = 0;
            this.ringAccepts = 0;
            this.ignoreAccepts = 0;
            this.ringRejects = 0;
            this.ignoreRejects = 0;
        }
//
//        public void addAccept() {
//            this.noOfAccepts++;
//        }

        public void addAccept(Action action) {
            this.noOfAccepts++;
            if (action == Action.RING) ringAccepts++;
            else ignoreAccepts++;
        }
//
//        public void addReject() {
//            this.noOfRejects++;
//        }

        public void addReject(Action action) {
            this.noOfRejects++;
            if (action == Action.RING) ringRejects++;
            else ignoreRejects++;
        }

        @Override
        public String toString() {
            return String.format("[total: %d, accepts: %d, rejects: %d, cohesion: %f, ringAccepts: %d, " +
                            "ignoreAccepts: %d, " +
                            "ringRejects: %d, ignoreRejects: %d]",
                    noOfAccepts + noOfRejects, noOfAccepts, noOfRejects,
                    ((double)noOfAccepts) / (noOfAccepts + noOfRejects) * 100, ringAccepts,
                    ignoreAccepts, ringRejects, ignoreRejects);
        }
    }

    public static class Statistics {
        public StatisticsUtil globalStats;
        public Map<Location, StatisticsUtil> regionalStats;

        public Statistics() {
            this.globalStats = new StatisticsUtil();
            this.regionalStats = new LinkedHashMap<>();
            for (Location location : Location.values()) {
                this.regionalStats.put(location, new StatisticsUtil());
            }
        }

        public void addAccept(Action action) {
            this.globalStats.addAccept(action);
        }

        public void addAccept(Action action, Location location) {
            this.globalStats.addAccept(action);
            this.regionalStats.get(location).addAccept(action);
        }

        public void addReject(Action action) {
            this.globalStats.addReject(action);
        }

        public void addReject(Action action, Location location) {
            this.globalStats.addReject(action);
            this.regionalStats.get(location).addReject(action);
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("===========================\n");
            builder.append(this.globalStats.toString()).append("\n");
            for (Location location : Location.values()) {
                builder.append(String.format("%s stats: %s\n", location.name(),
                        this.regionalStats.get(location).toString()));
            }
            builder.append("===========================\n");
            return builder.toString();
        }
    }
}
