package service;

import java.util.List;
import model.PlayerPerformance;

public class BowlingConsistencyStrategy implements ConsistencyStrategy {
    @Override
    public double compute(List<PlayerPerformance> performances) {
        if (performances == null || performances.isEmpty()) return 0;
        double avgWkts = performances.stream().mapToInt(PlayerPerformance::getWicketsTaken).average().orElse(0);
        double avgEconomy = performances.stream()
                .filter(p -> p.getOversBowled() > 0)
                .mapToDouble(p -> p.getRunsConceded() / p.getOversBowled())
                .average().orElse(0);
        return avgWkts - avgEconomy;
    }
}
