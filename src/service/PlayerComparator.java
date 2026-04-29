package service;

import model.Player;
import model.PlayerPerformance;

public class PlayerComparator {

    private PerformanceAnalyzer analyzer = new PerformanceAnalyzer();

    public ComparisonResult compareCareer(Player p1, Player p2) {
        double c1 = analyzer.calculateConsistency(p1);
        double c2 = analyzer.calculateConsistency(p2);
        return new ComparisonResult(c1, c2, p1, p2);
    }

    public ComparisonResult compareFormat(Player p1, Player p2, String format) {
        double i1 = analyzer.calculateImpact(p1, format);
        double i2 = analyzer.calculateImpact(p2, format);
        return new ComparisonResult(i1, i2, p1, p2, format);
    }

    public ComparisonResult compareRuns(Player p1, Player p2) {
        int r1 = p1.getPerformances().stream().mapToInt(PlayerPerformance::getRunsScored).sum();
        int r2 = p2.getPerformances().stream().mapToInt(PlayerPerformance::getRunsScored).sum();
        return new ComparisonResult(r1, r2, p1, p2, "runs");
    }

    public ComparisonResult compareWickets(Player p1, Player p2) {
        int w1 = p1.getPerformances().stream().mapToInt(PlayerPerformance::getWicketsTaken).sum();
        int w2 = p2.getPerformances().stream().mapToInt(PlayerPerformance::getWicketsTaken).sum();
        return new ComparisonResult(w1, w2, p1, p2, "wickets");
    }
}
