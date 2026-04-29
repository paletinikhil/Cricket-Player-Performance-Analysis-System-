package model;

public class YearlyStats {
    private int year;
    private int runs;
    private int wickets;
    private int matches;

    public YearlyStats(int year, int runs, int wickets, int matches) {
        this.year = year;
        this.runs = runs;
        this.wickets = wickets;
        this.matches = matches;
    }

    public void accumulate(PlayerPerformance perf) {
        this.matches++;
        this.runs += perf.getRunsScored();
        this.wickets += perf.getWicketsTaken();
    }

    public int getYear() { return year; }
    public int getRuns() { return runs; }
    public int getWickets() { return wickets; }
    public int getMatches() { return matches; }
}
