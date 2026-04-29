package service;

import java.util.List;
import model.PlayerPerformance;

public class AllRounderConsistencyStrategy implements ConsistencyStrategy {
    @Override
    public double compute(List<PlayerPerformance> performances) {
        if (performances == null || performances.isEmpty()) return 0;
        double battingAverage = performances.stream().mapToInt(PlayerPerformance::getRunsScored).average().orElse(0);
        double wicketsPerMatch = performances.stream().mapToInt(PlayerPerformance::getWicketsTaken).average().orElse(0);
        return 0.5 * battingAverage + 0.5 * wicketsPerMatch;
    }
}
