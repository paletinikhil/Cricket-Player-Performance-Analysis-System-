package model;

public class PlayerPerformance {

    private int runs;
    private int wickets;
    private int ballsFaced;
    private double overs;
    private int runsConceded;
    private Match match;

    public PlayerPerformance(int runs, int wickets, int ballsFaced,
                             double overs, int runsConceded, Match match) {
        this.runs = runs;
        this.wickets = wickets;
        this.ballsFaced = ballsFaced;
        this.overs = overs;
        this.runsConceded = runsConceded;
        this.match = match;
    }

    public int getRuns() { return runs; }
    public int getWickets() { return wickets; }
    public double getOvers() { return overs; }
    public int getRunsConceded() { return runsConceded; }
    public Match getMatch() { return match; }
}
