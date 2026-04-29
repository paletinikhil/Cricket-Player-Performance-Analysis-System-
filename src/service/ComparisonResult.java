package service;

import model.Player;

public class ComparisonResult {
    private double value1;
    private double value2;
    private Player p1;
    private Player p2;
    private String metric;

    public ComparisonResult(double value1, double value2, Player p1, Player p2) {
        this(value1, value2, p1, p2, null);
    }

    public ComparisonResult(double value1, double value2, Player p1, Player p2, String metric) {
        this.value1 = value1;
        this.value2 = value2;
        this.p1 = p1;
        this.p2 = p2;
        this.metric = metric;
    }

    public Player better() {
        return value1 >= value2 ? p1 : p2;
    }

    public String getMetric() {
        return metric;
    }

    public double getValue1() {
        return value1;
    }

    public double getValue2() {
        return value2;
    }

    public Player getP1() {
        return p1;
    }

    public Player getP2() {
        return p2;
    }
}