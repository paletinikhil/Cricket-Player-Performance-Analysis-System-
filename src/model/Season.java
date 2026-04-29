package model;

public class Season {

    private int year;

    public Season(int year) {
        this.year = year;
    }

    public int getYear() {
        return year;
    }

    // placeholder: compute summary from performances in that season
    public String getSeasonSummary() {
        return "Summary for season " + year;
    }

    // return top players for season (could be based on repository lookup)
    public java.util.List<String> getTopPlayers() {
        return java.util.Collections.emptyList();
    }
}
