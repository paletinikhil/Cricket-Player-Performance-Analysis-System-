package model;

import java.time.LocalDate;

public class Match {

    private int matchId;
    private LocalDate date;
    private String format;
    private String tournament;        // added to support tournament stats
    private String series;            // added to support bilateral series
    private String team1;
    private String team2;
    private Integer team1Runs;
    private Integer team1Wickets;
    private Integer team2Runs;
    private Integer team2Wickets;
    private String tossWinner;
    private String battingFirst;
    private String battingSecond;
    private String matchWinner;

    public Match(int matchId, LocalDate date, String format, String tournament) {
        this(matchId, date, format, tournament, null, null, null, null, null, null, null, null, null, null, null);
    }

    public Match(int matchId, LocalDate date, String format, String tournament,
                 String team1, String team2,
                 Integer team1Runs, Integer team1Wickets,
                 Integer team2Runs, Integer team2Wickets,
                 String tossWinner, String battingFirst, String battingSecond, String matchWinner) {
        this(matchId, date, format, tournament, null, team1, team2, team1Runs, team1Wickets, team2Runs, team2Wickets, tossWinner, battingFirst, battingSecond, matchWinner);
    }

    public Match(int matchId, LocalDate date, String format, String tournament, String series,
                 String team1, String team2,
                 Integer team1Runs, Integer team1Wickets,
                 Integer team2Runs, Integer team2Wickets,
                 String tossWinner, String battingFirst, String battingSecond, String matchWinner) {
        this.matchId = matchId;
        this.date = date;
        this.format = format;
        this.tournament = tournament;
        this.series = series;
        this.team1 = team1;
        this.team2 = team2;
        this.team1Runs = team1Runs;
        this.team1Wickets = team1Wickets;
        this.team2Runs = team2Runs;
        this.team2Wickets = team2Wickets;
        this.tossWinner = tossWinner;
        this.battingFirst = battingFirst;
        this.battingSecond = battingSecond;
        this.matchWinner = matchWinner;
    }

    public int getMatchId() { return matchId; }
    public LocalDate getDate() { return date; }
    public String getFormat() { return format; }
    public String getTournament() { return tournament; }
    public String getSeries() { return series; }
    public String getTeam1() { return team1; }
    public String getTeam2() { return team2; }
    public Integer getTeam1Runs() { return team1Runs; }
    public Integer getTeam1Wickets() { return team1Wickets; }
    public Integer getTeam2Runs() { return team2Runs; }
    public Integer getTeam2Wickets() { return team2Wickets; }
    public String getTossWinner() { return tossWinner; }
    public String getBattingFirst() { return battingFirst; }
    public String getBattingSecond() { return battingSecond; }
    public String getMatchWinner() { return matchWinner; }
}
