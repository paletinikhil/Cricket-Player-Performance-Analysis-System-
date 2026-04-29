package model;

public class TournamentStats {

    private String tournamentName;
    private int runs;
    private int wickets;
    private int matches;

    public TournamentStats(String tournamentName, int runs, int wickets) {
        this.tournamentName = tournamentName;
        this.runs = runs;
        this.wickets = wickets;
        this.matches = 0;
    }

    public void accumulate(PlayerPerformance perf) {
        this.matches++;
        this.runs += perf.getRunsScored();
        this.wickets += perf.getWicketsTaken();
    }

    public String getTournamentName() { return tournamentName; }
    public int getRuns() { return runs; }
    public int getWickets() { return wickets; }
    public int getMatches() { return matches; }
}
