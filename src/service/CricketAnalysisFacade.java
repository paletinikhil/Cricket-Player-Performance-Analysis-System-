package service;

import java.util.List;
import model.Player;
import repository.IPlayerRepository;

// MVC + GRASP (Controller): orchestrates use-cases between UI/controller and domain services.
// Design Pattern: Facade (single entry for application use-cases).
// SOLID (DIP): depends on IPlayerRepository abstraction.
public class CricketAnalysisFacade {
    private final IPlayerRepository playerRepository;
    private final PlayerSearchService searchService;
    private final PerformanceAnalyzer analyzer;
    private final PlayerComparator comparator;
    private final ReportGenerator reportGenerator;

    public CricketAnalysisFacade(
            IPlayerRepository playerRepository,
            PlayerSearchService searchService,
            PerformanceAnalyzer analyzer,
            PlayerComparator comparator,
            ReportGenerator reportGenerator) {
        this.playerRepository = playerRepository;
        this.searchService = searchService;
        this.analyzer = analyzer;
        this.comparator = comparator;
        this.reportGenerator = reportGenerator;
    }

    public List<Player> getAllPlayers() {
        return playerRepository.findAll();
    }

    public List<Player> searchPlayersByName(String name) {
        return searchService.searchByName(playerRepository.findAll(), name);
    }

    public double analyzeConsistency(Player player) {
        return analyzer.calculateConsistency(player);
    }

    public double analyzeRecentForm(Player player, String format) {
        return analyzer.recentForm(player, format);
    }

    public ComparisonResult compareCareer(Player p1, Player p2) {
        return comparator.compareCareer(p1, p2);
    }

    public String buildPlayerReport(Player player) {
        return reportGenerator.generatePlayerReportString(player);
    }
}
