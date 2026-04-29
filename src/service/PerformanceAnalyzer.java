package service;

import java.util.Comparator;
import java.util.List;
import model.Player;
import model.PlayerPerformance;

public class PerformanceAnalyzer {

    // Career Consistency
    public double calculateConsistency(Player player) {
        if (player.getPerformances() == null || player.getPerformances().isEmpty())
            return 0;
        String role = normalizeRole(player.getRole());
        // Design Pattern: Strategy + Factory Method.
        // SOLID (OCP): add new role behavior without editing calculateConsistency logic.
        ConsistencyStrategy strategy = createConsistencyStrategy(role);
        return strategy.compute(player.getPerformances());
    }

    private ConsistencyStrategy createConsistencyStrategy(String role) {
        if ("batsman".equals(role)) return new BattingConsistencyStrategy();
        if ("bowler".equals(role)) return new BowlingConsistencyStrategy();
        if ("all_rounder".equals(role)) return new AllRounderConsistencyStrategy();
        return performances -> 0;
    }

    private double battingConsistency(List<PlayerPerformance> list) {
        double[] runs = list.stream().mapToDouble(PlayerPerformance::getRunsScored).toArray();
        double meanRuns = mean(runs);
        double stdRuns = standardDeviation(runs);
        return meanRuns - stdRuns;
    }

    private double bowlingConsistency(List<PlayerPerformance> list) {
        double avgWkts = list.stream().mapToInt(PlayerPerformance::getWicketsTaken).average().orElse(0);
        double avgEconomy = list.stream()
                .filter(p -> p.getOversBowled() > 0)
                .mapToDouble(p -> p.getRunsConceded() / p.getOversBowled())
                .average().orElse(0);
        return avgWkts - avgEconomy;
    }

    private double allRounderConsistency(List<PlayerPerformance> list) {
        double battingAverage = list.stream().mapToInt(PlayerPerformance::getRunsScored).average().orElse(0);
        double wicketsPerMatch = list.stream().mapToInt(PlayerPerformance::getWicketsTaken).average().orElse(0);
        return 0.5 * battingAverage + 0.5 * wicketsPerMatch;
    }

    private double standardDeviation(double[] data) {
        if (data.length == 0) return 0;
        double mean = 0;
        for (double d : data) mean += d;
        mean /= data.length;
        double sum = 0;
        for (double d : data) sum += Math.pow(d - mean, 2);
        return Math.sqrt(sum / data.length);
    }

    // Impact per format (or all formats if format is null)
    public double calculateImpact(Player player, String format) {
        if (player.getPerformances() == null || player.getPerformances().isEmpty())
            return 0;
        List<PlayerPerformance> list = player.getPerformances().stream()
                .filter(p -> format == null || p.getMatch().getFormat().equalsIgnoreCase(format))
                .toList();
        if (list.isEmpty()) return 0;
        String role = normalizeRole(player.getRole());
        double battingImpact = list.stream().mapToDouble(this::battingImpactForMatch).average().orElse(0);
        double bowlingImpact = list.stream().mapToDouble(this::bowlingImpactForMatch).average().orElse(0);

        if ("batsman".equals(role)) {
            return battingImpact;
        } else if ("bowler".equals(role)) {
            return bowlingImpact;
        } else if ("all_rounder".equals(role)) {
            return 0.5 * battingImpact + 0.5 * bowlingImpact;
        }
        return 0;
    }

    // Recent form (last 15 matches, optionally by format)
    public double recentForm(Player player, String format) {
        List<PlayerPerformance> recent = player.getPerformances().stream()
                .filter(p -> format == null || p.getMatch().getFormat().equalsIgnoreCase(format))
                .sorted(Comparator.comparing(p -> p.getMatch().getDate(), Comparator.reverseOrder()))
                .limit(15)
                .toList();

        if (recent.isEmpty()) return 0;
        String role = normalizeRole(player.getRole());
        if ("batsman".equals(role)) {
            return recent.stream().mapToInt(PlayerPerformance::getRunsScored).average().orElse(0);
        }
        if ("bowler".equals(role)) {
            double wicketsPerMatch = recent.stream().mapToInt(PlayerPerformance::getWicketsTaken).average().orElse(0);
            double economy = recent.stream()
                    .filter(p -> p.getOversBowled() > 0)
                    .mapToDouble(p -> p.getRunsConceded() / p.getOversBowled())
                    .average().orElse(0);
            return wicketsPerMatch - economy;
        }

        double recentBattingAverage = recent.stream().mapToInt(PlayerPerformance::getRunsScored).average().orElse(0);
        double recentWickets = recent.stream().mapToInt(PlayerPerformance::getWicketsTaken).average().orElse(0);
        return (recentBattingAverage * 0.6) + (recentWickets * 0.4);
    }

    private String normalizeRole(String role) {
        if (role == null) return "";
        String normalized = role.trim().toLowerCase().replace('-', '_').replace(' ', '_');
        if (normalized.equals("allrounder")) return "all_rounder";
        return normalized;
    }

    private double battingImpactForMatch(PlayerPerformance p) {
        double strikeRate = p.getBallsFaced() > 0 ? p.getRunsScored() * 100.0 / p.getBallsFaced() : 0;
        double strikeRateScore = Math.min(100, strikeRate / 2.0);

        int teamRuns = teamRunsForPerformance(p);
        double base;
        if (teamRuns > 0) {
            double runShare = (p.getRunsScored() * 100.0) / teamRuns;
            base = 0.7 * runShare + 0.3 * strikeRateScore;
        } else {
            base = (p.getRunsScored() * strikeRate) / 100.0;
        }
        return base * contextualImpactMultiplier(p);
    }

    private double bowlingImpactForMatch(PlayerPerformance p) {
        double economy = p.getOversBowled() > 0 ? p.getRunsConceded() / p.getOversBowled() : 10;
        double economyScore = Math.max(0, Math.min(100, (10 - economy) * 10));

        int teamWickets = teamWicketsForPerformance(p);
        double base;
        if (teamWickets > 0) {
            double wicketShare = (p.getWicketsTaken() * 100.0) / teamWickets;
            base = 0.7 * wicketShare + 0.3 * economyScore;
        } else {
            base = (p.getWicketsTaken() * 20.0) - (economy * 5.0);
        }
        return base * contextualImpactMultiplier(p);
    }

    private double contextualImpactMultiplier(PlayerPerformance p) {
        if (p.getMatch() == null || p.getTeamName() == null || p.getTeamName().isBlank()) return 1.0;

        String playerTeam = p.getTeamName().trim();
        double multiplier = 1.0;

        if (p.getMatch().getMatchWinner() != null && playerTeam.equalsIgnoreCase(p.getMatch().getMatchWinner())) {
            multiplier += 0.10;
        }
        if (p.getMatch().getBattingSecond() != null && playerTeam.equalsIgnoreCase(p.getMatch().getBattingSecond())) {
            multiplier += 0.05;
        }
        if (p.getMatch().getTossWinner() != null && playerTeam.equalsIgnoreCase(p.getMatch().getTossWinner())) {
            multiplier += 0.03;
        }

        return multiplier;
    }

    private int teamRunsForPerformance(PlayerPerformance p) {
        if (p.getMatch() == null || p.getTeamName() == null || p.getTeamName().isBlank()) return 0;
        String playerTeam = p.getTeamName().trim();
        if (p.getMatch().getTeam1() != null && playerTeam.equalsIgnoreCase(p.getMatch().getTeam1())) {
            return p.getMatch().getTeam1Runs() == null ? 0 : p.getMatch().getTeam1Runs();
        }
        if (p.getMatch().getTeam2() != null && playerTeam.equalsIgnoreCase(p.getMatch().getTeam2())) {
            return p.getMatch().getTeam2Runs() == null ? 0 : p.getMatch().getTeam2Runs();
        }
        return 0;
    }

    private int teamWicketsForPerformance(PlayerPerformance p) {
        if (p.getMatch() == null || p.getTeamName() == null || p.getTeamName().isBlank()) return 0;
        String playerTeam = p.getTeamName().trim();
        if (p.getMatch().getTeam1() != null && playerTeam.equalsIgnoreCase(p.getMatch().getTeam1())) {
            return p.getMatch().getTeam1Wickets() == null ? 0 : p.getMatch().getTeam1Wickets();
        }
        if (p.getMatch().getTeam2() != null && playerTeam.equalsIgnoreCase(p.getMatch().getTeam2())) {
            return p.getMatch().getTeam2Wickets() == null ? 0 : p.getMatch().getTeam2Wickets();
        }
        return 0;
    }

    private double mean(double[] data) {
        if (data.length == 0) return 0;
        double sum = 0;
        for (double d : data) sum += d;
        return sum / data.length;
    }

    // Public accessors for per-match impact details
    public double battingImpactPublic(PlayerPerformance p) { return battingImpactForMatch(p); }
    public double bowlingImpactPublic(PlayerPerformance p) { return bowlingImpactForMatch(p); }
    public double contextMultiplierPublic(PlayerPerformance p) { return contextualImpactMultiplier(p); }
}
