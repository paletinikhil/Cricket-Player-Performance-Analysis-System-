package main;

import java.util.List;
import java.util.Scanner;
import model.Player;
import repository.AuthRepository;
import repository.IPlayerRepository;
import repository.PlayerRepository;
import service.*;

public class Mainapp {

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);
        AuthRepository authRepo = new AuthRepository();
        // SOLID (DIP): UI/controller depends on repository abstraction, not concrete implementation.
        IPlayerRepository repo = new PlayerRepository();
        PlayerSearchService searchService = new PlayerSearchService();
        PerformanceAnalyzer analyzer = new PerformanceAnalyzer();
        PlayerComparator comparator = new PlayerComparator();
        ReportGenerator report = new ReportGenerator();
        // MVC + GRASP Controller via Facade: keeps application use-cases in one cohesive service.
        CricketAnalysisFacade facade = new CricketAnalysisFacade(repo, searchService, analyzer, comparator, report);

        System.out.println("Welcome to Cricket Player Performance Analysis System");
        System.out.print("Username: ");
        String user = sc.next();
        System.out.print("Password: ");
        String pass = sc.next();

        model.User logged = authRepo.login(user, pass);
        if (logged == null) {
            System.out.println("Invalid credentials");
            System.exit(1);
        }

        boolean isAdmin = "ADMIN".equalsIgnoreCase(logged.getRole());
        System.out.println("Logged in as " + logged.getName() + " (" + logged.getRole() + ")");

        while (true) {
            System.out.println("\n--- main menu ---");
            System.out.println("1. View All Players");
            System.out.println("2. Search Player");
            System.out.println("3. Analyze Player");
            System.out.println("4. Compare Players");
            System.out.println("5. Generate Report");
            if (isAdmin) {
                System.out.println("6. Add Player");
                System.out.println("7. Add Match/Performance");
                System.out.println("8. Logout");
            } else {
                System.out.println("6. Logout");
            }

            int choice = sc.nextInt();
            List<Player> players = facade.getAllPlayers();

            switch (choice) {
                case 1 -> players.forEach(p -> System.out.println(p.getPlayerId() + " - " + p.getName()));
                case 2 -> {
                    System.out.println("Enter name:");
                    String name = sc.next();
                    facade.searchPlayersByName(name)
                            .forEach(p -> System.out.println("Found: " + p.getName()));
                }
                case 3 -> {
                    System.out.println("Enter Player ID:");
                    int id = sc.nextInt();
                    Player player = players.stream()
                            .filter(p -> p.getPlayerId() == id)
                            .findFirst().orElse(null);
                    if (player != null) {
                        double consistency = facade.analyzeConsistency(player);
                        double recent = facade.analyzeRecentForm(player, null);
                        System.out.println("Consistency: " + consistency);
                        System.out.println("Recent Form (all formats): " + recent);
                    }
                }
                case 4 -> {
                    System.out.println("Enter Player1 ID:");
                    int id1 = sc.nextInt();
                    System.out.println("Enter Player2 ID:");
                    int id2 = sc.nextInt();
                    Player p1 = players.stream().filter(p -> p.getPlayerId() == id1).findFirst().orElse(null);
                    Player p2 = players.stream().filter(p -> p.getPlayerId() == id2).findFirst().orElse(null);
                    if (p1 != null && p2 != null) {
                        ComparisonResult res = facade.compareCareer(p1, p2);
                        System.out.println("Better overall: " + res.better().getName());
                    }
                }
                case 5 -> {
                    System.out.println("Enter Player ID:");
                    int id = sc.nextInt();
                    Player player = players.stream()
                            .filter(p -> p.getPlayerId() == id)
                            .findFirst().orElse(null);
                    if (player != null)
                        System.out.println(facade.buildPlayerReport(player));
                }
                case 6 -> {
                    if (isAdmin) {
                        System.out.print("Name: ");
                        String name = sc.next();
                        System.out.print("Country: ");
                        String country = sc.next();
                        System.out.print("Role (Batsman/Bowler/All-Rounder): ");
                        String rolep = sc.next();
                        // simplistic insertion
                        // TODO: implement repository.save
                        System.out.println("(player creation not implemented)");
                    } else {
                        System.out.println("Logging out");
                        System.exit(0);
                    }
                    break;
                }
                case 7 -> {
                    if (isAdmin) {
                        System.out.println("This option would allow recording a match/performance.");
                    }
                    break;
                }
                case 8 -> {
                    if (isAdmin) {
                        System.out.println("Logging out");
                        System.exit(0);
                    }
                    break;
                }
            }
        }
    }
}
