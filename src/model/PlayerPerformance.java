package model;

public class PlayerPerformance {

    private int runsScored;
    private int wicketsTaken;
    private int ballsFaced;
    private double oversBowled;
    private int runsConceded;
    private String teamName;
    private Match match;

    public PlayerPerformance(int runsScored, int wicketsTaken, int ballsFaced,
                             double oversBowled, int runsConceded, Match match) {
        this(runsScored, wicketsTaken, ballsFaced, oversBowled, runsConceded, null, match);
    }

    public PlayerPerformance(int runsScored, int wicketsTaken, int ballsFaced,
                             double oversBowled, int runsConceded, String teamName, Match match) {
        this.runsScored = runsScored;
        this.wicketsTaken = wicketsTaken;
        this.ballsFaced = ballsFaced;
        this.oversBowled = oversBowled;
        this.runsConceded = runsConceded;
        this.teamName = teamName;
        this.match = match;
    }

    public int getRunsScored() { return runsScored; }
    public int getWicketsTaken() { return wicketsTaken; }
    public int getBallsFaced() { return ballsFaced; }
    public double getOversBowled() { return oversBowled; }
    public int getRunsConceded() { return runsConceded; }
    public String getTeamName() { return teamName; }
    public Match getMatch() { return match; }

    // basic impact calculation; more detailed formulas live in service
    public double calculateImpact() {
        if (match == null) return 0;
        // simple combined metric
        return runsScored + wicketsTaken * 20 - runsConceded;
    }
}
