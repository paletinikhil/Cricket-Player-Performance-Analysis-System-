package model;

import java.util.List;

public class Player {

    private int playerId;
    private String name;
    private String country;
    private String role;
    private String battingStyle;
    private String bowlingStyle;
    private int debutYear;
    private List<PlayerPerformance> performances;

    public Player(int playerId, String name, String country, String role) {
        this.playerId = playerId;
        this.name = name;
        this.country = country;
        this.role = role;
    }

    public Player(int playerId, String name, String country, String role, String battingStyle, String bowlingStyle, int debutYear) {
        this.playerId = playerId;
        this.name = name;
        this.country = country;
        this.role = role;
        this.battingStyle = battingStyle;
        this.bowlingStyle = bowlingStyle;
        this.debutYear = debutYear;
    }

    public int getPlayerId() { return playerId; }
    public String getName() { return name; }
    public String getCountry() { return country; }
    public String getRole() { return role; }
    public String getBattingStyle() { return battingStyle; }
    public void setBattingStyle(String battingStyle) { this.battingStyle = battingStyle; }
    public String getBowlingStyle() { return bowlingStyle; }
    public void setBowlingStyle(String bowlingStyle) { this.bowlingStyle = bowlingStyle; }
    public int getDebutYear() { return debutYear; }
    public void setDebutYear(int debutYear) { this.debutYear = debutYear; }

    public void setPerformances(List<PlayerPerformance> performances) {
        this.performances = performances;
    }

    public List<PlayerPerformance> getPerformances() {
        return performances;
    }

    // derived helper methods reflecting class diagram operations
    public CareerStats getCareerStats() {
        if (performances == null) return new CareerStats(0,0);
        int runs = performances.stream().mapToInt(PlayerPerformance::getRunsScored).sum();
        int wickets = performances.stream().mapToInt(PlayerPerformance::getWicketsTaken).sum();
        return new CareerStats(runs, wickets);
    }

    public java.util.Map<Integer, YearlyStats> getStatsByYear() {
        java.util.Map<Integer, YearlyStats> map = new java.util.HashMap<>();
        if (performances == null) return map;
        for (PlayerPerformance p : performances) {
            int year = p.getMatch().getDate().getYear();
            map.computeIfAbsent(year, y -> new YearlyStats(y,0,0,0))
               .accumulate(p);
        }
        return map;
    }

    public java.util.Map<String, TournamentStats> getStatsByTournament() {
        java.util.Map<String, TournamentStats> map = new java.util.HashMap<>();
        if (performances == null) return map;
        for (PlayerPerformance p : performances) {
            String tour = p.getMatch().getTournament();
            map.computeIfAbsent(tour, t -> new TournamentStats(t,0,0))
               .accumulate(p);
        }
        return map;
    }

    public java.util.Map<Format, java.util.List<PlayerPerformance>> getStatsByFormat() {
        java.util.Map<Format, java.util.List<PlayerPerformance>> map = new java.util.HashMap<>();
        if (performances == null) return map;
        for (PlayerPerformance p : performances) {
            Format fmt = Format.valueOf(p.getMatch().getFormat().toUpperCase());
            map.computeIfAbsent(fmt, f -> new java.util.ArrayList<>()).add(p);
        }
        return map;
    }
}
