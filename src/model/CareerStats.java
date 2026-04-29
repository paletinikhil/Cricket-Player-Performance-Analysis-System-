package model;

public class CareerStats {

    private int totalRuns;
    private int totalWickets;

    public CareerStats(int totalRuns, int totalWickets) {
        this.totalRuns = totalRuns;
        this.totalWickets = totalWickets;
    }

    public int getTotalRuns() { return totalRuns; }
    public int getTotalWickets() { return totalWickets; }
}
