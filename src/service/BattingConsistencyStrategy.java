package service;

import java.util.List;
import model.PlayerPerformance;

public class BattingConsistencyStrategy implements ConsistencyStrategy {
    @Override
    public double compute(List<PlayerPerformance> performances) {
        if (performances == null || performances.isEmpty()) return 0;
        double[] runs = performances.stream().mapToDouble(PlayerPerformance::getRunsScored).toArray();
        double mean = 0;
        for (double d : runs) mean += d;
        mean /= runs.length;
        double variance = 0;
        for (double d : runs) variance += Math.pow(d - mean, 2);
        double std = Math.sqrt(variance / runs.length);
        return mean - std;
    }
}
