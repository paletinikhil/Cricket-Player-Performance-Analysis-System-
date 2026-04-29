
package model;

public class YearlyStats {

    private int year;
    private int runs;
    private int wickets;

    public YearlyStats(int year, int runs, int wickets) {
        this.year = year;
        this.runs = runs;
        this.wickets = wickets;
    }

    public int getYear() { return year; }
}
