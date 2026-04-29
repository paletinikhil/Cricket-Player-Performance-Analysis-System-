package service;

import model.CareerStats;
import model.Player;

public class ReportGenerator {

    private PerformanceAnalyzer analyzer = new PerformanceAnalyzer();

    public void generatePlayerReport(Player player) {
        System.out.println(generatePlayerReportString(player));
    }

    public String generatePlayerReportString(Player player) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n===== PLAYER REPORT =====\n");
        sb.append("Name: ").append(player.getName()).append("\n");
        sb.append("Role: ").append(player.getRole()).append("\n");
        sb.append("Country: ").append(player.getCountry()).append("\n");
        sb.append("Matches Played: ").append(player.getPerformances() == null ? 0 : player.getPerformances().size()).append("\n");

        CareerStats cs = player.getCareerStats();
        sb.append("Total Runs: ").append(cs.getTotalRuns()).append("\n");
        sb.append("Total Wickets: ").append(cs.getTotalWickets()).append("\n");

        double consistency = analyzer.calculateConsistency(player);
        sb.append("Consistency Score: ").append(String.format("%.2f", consistency)).append("\n");

        sb.append("-- Format breakdown --\n");
        player.getStatsByFormat().forEach((fmt, perfs) -> {
            sb.append(fmt).append(":\n");
            double impact = analyzer.calculateImpact(player, fmt.name());
            sb.append("   Impact: ").append(String.format("%.2f", impact)).append("\n");
            double recent = analyzer.recentForm(player, fmt.name());
            sb.append("   Recent form (last15 role-based): ").append(String.format("%.2f", recent)).append("\n");
        });
        return sb.toString();
    }
}
