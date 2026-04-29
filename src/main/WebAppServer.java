package main;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.awt.Desktop;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.*;
import java.util.List;
import model.Player;
import model.PlayerPerformance;
import model.User;
import repository.AuthRepository;
import repository.DatabaseConnection;
import repository.PlayerRepository;
import service.PerformanceAnalyzer;

public class WebAppServer {

    // MVC: this class is part of Controller layer (HTTP endpoints).
    // GRASP Controller: request coordination is handled here, while business rules stay in service classes.
    static String nullToEmpty(String value) {
        return value == null ? "" : value.replace("\"", "\\\"");
    }

    public static void main(String[] args) throws Exception {
        ensureSchemaColumns();
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", new StaticHandler());
        server.createContext("/api/login", new LoginHandler());
        server.createContext("/api/register", new RegisterHandler());
        server.createContext("/api/players", new PlayersHandler());
        server.createContext("/api/players/stats", new PlayersWithStatsHandler());
        server.createContext("/api/search", new SearchHandler());
        server.createContext("/api/analyze", new AnalyzeHandler());
        server.createContext("/api/compare", new CompareHandler());
        server.createContext("/api/report", new ReportHandler());
        server.createContext("/api/stats/career", new CareerStatsHandler());
        server.createContext("/api/stats/yearly", new YearlyStatsHandler());
        server.createContext("/api/stats/tournament", new TournamentStatsHandler());
        server.createContext("/api/filter/year", new FilterByYearHandler());
        server.createContext("/api/filter/format", new FilterByFormatHandler());
        server.createContext("/api/filter/tournament", new FilterByTournamentHandler());
        server.createContext("/api/filter/country", new FilterByCountryHandler());
        server.createContext("/api/filter/role", new FilterByRoleHandler());
        server.createContext("/api/matches", new MatchesHandler());
        server.createContext("/api/addPlayer", new AddPlayerHandler());
        server.createContext("/api/addMatch", new AddMatchHandler());
        server.createContext("/api/addPerformance", new AddPerformanceHandler());
        server.createContext("/api/recalculateStats", new RecalculateStatsHandler());
        server.createContext("/api/deletePlayer", new DeletePlayerHandler());
        server.createContext("/api/deleteMatch", new DeleteMatchHandler());
        server.createContext("/api/performanceGraph", new PerformanceGraphHandler());
        server.createContext("/api/sortPlayers", new SortPlayersHandler());
        server.createContext("/api/seasonReport", new SeasonReportHandler());
        server.createContext("/api/comparisonReport", new ComparisonReportHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("Server started on http://localhost:8080/");
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(new URI("http://localhost:8080/login.html"));
        }
    }

    static void ensureSchemaColumns() {
        try (Connection con = DatabaseConnection.getConnection(); Statement st = con.createStatement()) {
            ensureColumnExists(con, st, "matches", "team1_runs", "INT NULL");
            ensureColumnExists(con, st, "matches", "team1_wickets", "INT NULL");
            ensureColumnExists(con, st, "matches", "team2_runs", "INT NULL");
            ensureColumnExists(con, st, "matches", "team2_wickets", "INT NULL");
            ensureColumnExists(con, st, "matches", "toss_winner", "VARCHAR(100) NULL");
            ensureColumnExists(con, st, "matches", "batting_first", "VARCHAR(100) NULL");
            ensureColumnExists(con, st, "matches", "batting_second", "VARCHAR(100) NULL");
            ensureColumnExists(con, st, "matches", "match_winner", "VARCHAR(100) NULL");
            ensureColumnExists(con, st, "matches", "series", "VARCHAR(100) NULL");
            ensureColumnExists(con, st, "player_performance", "team_name", "VARCHAR(100) NULL");
        } catch (Exception e) {
            System.err.println("Schema migration warning: " + e.getMessage());
        }
    }

    static void ensureColumnExists(Connection con, Statement st, String tableName, String columnName, String columnDefinition) throws SQLException {
        DatabaseMetaData meta = con.getMetaData();
        String catalog = con.getCatalog();
        boolean exists = false;
        try (ResultSet rs = meta.getColumns(catalog, null, tableName, columnName)) {
            exists = rs.next();
        }
        if (!exists) {
            st.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDefinition);
        }
    }

    static class StaticHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            URI uri = exchange.getRequestURI();
            String path = uri.getPath();
            if (path.equals("/")) path = "/login.html";
            File f = new File("src/main/web" + path).getCanonicalFile();
            if (!f.getPath().startsWith(new File("src/main/web").getCanonicalPath())) {
                exchange.sendResponseHeaders(403, -1);
                return;
            }
            if (!f.exists()) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            byte[] bytes = Files.readAllBytes(f.toPath());
            exchange.getResponseHeaders().add("Content-Type", guessContentType(f.getName()));
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }

        private String guessContentType(String name) {
            if (name.endsWith(".html")) return "text/html";
            if (name.endsWith(".css")) return "text/css";
            if (name.endsWith(".js")) return "application/javascript";
            return "application/octet-stream";
        }
    }

    static void sendText(HttpExchange ex, String text) throws IOException {
        byte[] bytes = text.getBytes();
        ex.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    static void sendJson(HttpExchange ex, String json) throws IOException {
        byte[] bytes = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    static String getQueryParam(String query, String key) {
        if (query == null || query.isEmpty()) return null;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int eq = pair.indexOf('=');
            if (eq <= 0) continue;
            String k = pair.substring(0, eq);
            if (k.equals(key)) {
                String v = pair.substring(eq + 1);
                return URLDecoder.decode(v, StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    static class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
                ex.sendResponseHeaders(405, -1);
                return;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(ex.getRequestBody()));
            String line = reader.readLine();
            if (line == null) {
                sendText(ex, "FAIL");
                return;
            }
            String[] parts = line.split("&");
            String user="", pass="", roleParam="";
            for(String p:parts){
                if(p.startsWith("user=")) user=URLDecoder.decode(p.substring(5), StandardCharsets.UTF_8);
                if(p.startsWith("pass=")) pass=URLDecoder.decode(p.substring(5), StandardCharsets.UTF_8);
                if(p.startsWith("role=")) roleParam=URLDecoder.decode(p.substring(5), StandardCharsets.UTF_8);
            }
            User u = new AuthRepository().login(user, pass);
            if (u != null) {
                // if client supplied a desired role, make sure it matches the one in DB
                if (!roleParam.isEmpty() && !roleParam.equalsIgnoreCase(u.getRole())) {
                    sendText(ex, "FAIL");
                } else {
                    // return actual role so frontend can adapt
                    sendText(ex, "OK|" + u.getRole().toLowerCase());
                }
            } else {
                sendText(ex, "FAIL");
            }
        }
    }

    static class RegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
                ex.sendResponseHeaders(405, -1);
                return;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(ex.getRequestBody()));
            String line = reader.readLine();
            if (line == null) {
                sendText(ex, "FAIL");
                return;
            }
            String[] parts = line.split("&");
            String username = "", password = "";
            for (String p : parts) {
                if (p.startsWith("username=")) username = URLDecoder.decode(p.substring(9), StandardCharsets.UTF_8);
                if (p.startsWith("password=")) password = URLDecoder.decode(p.substring(9), StandardCharsets.UTF_8);
            }
            
            if (username.isEmpty() || password.isEmpty()) {
                sendText(ex, "FAIL");
                return;
            }
            
            try (Connection con = DatabaseConnection.getConnection()) {
                // Check if username already exists
                String checkSql = "SELECT COUNT(*) FROM users WHERE username = ?";
                PreparedStatement checkPs = con.prepareStatement(checkSql);
                checkPs.setString(1, username);
                ResultSet checkRs = checkPs.executeQuery();
                if (checkRs.next() && checkRs.getInt(1) > 0) {
                    sendText(ex, "DUPLICATE");
                    return;
                }
                
                // Insert new user with role='user' (not admin)
                String insertSql = "INSERT INTO users (name, username, password, role) VALUES (?, ?, ?, 'user')";
                PreparedStatement insertPs = con.prepareStatement(insertSql);
                insertPs.setString(1, username); // name = username
                insertPs.setString(2, username);
                insertPs.setString(3, password);
                insertPs.executeUpdate();
                
                sendText(ex, "OK");
            } catch (Exception e) {
                System.err.println("RegisterHandler error: " + e.getMessage());
                sendText(ex, "FAIL");
            }
        }
    }

    static class PlayersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            List<Player> list = new PlayerRepository().findAll();
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (int i = 0; i < list.size(); i++) {
                Player p = list.get(i);
                sb.append("{\"playerId\":").append(p.getPlayerId())
                  .append(",\"name\":\"").append(p.getName()).append("\"")
                  .append(",\"country\":\"").append(p.getCountry()).append("\"")
                  .append(",\"role\":\"").append(p.getRole()).append("\"")
                  .append(",\"battingStyle\":\"").append(nullToEmpty(p.getBattingStyle())).append("\"")
                  .append(",\"bowlingStyle\":\"").append(nullToEmpty(p.getBowlingStyle())).append("\"")
                  .append(",\"debutYear\":").append(p.getDebutYear())
                  .append("}");
                if (i < list.size()-1) sb.append(",");
            }
            sb.append("]");
            ex.getResponseHeaders().add("Content-Type","application/json");
            sendText(ex, sb.toString());
        }
    }

    static class PlayersWithStatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            try (Connection con = DatabaseConnection.getConnection()) {
                String sql = """
                        SELECT p.player_id, p.name, p.country, p.role, p.batting_style, p.bowling_style,
                               COUNT(pp.performance_id) AS matches_played,
                               COALESCE(SUM(pp.runs_scored),0) AS total_runs,
                               COALESCE(SUM(pp.wickets_taken),0) AS total_wickets,
                               ROUND(COALESCE(AVG(CASE WHEN pp.balls_faced > 0 THEN pp.runs_scored * 100.0 / pp.balls_faced END),0),2) AS avg_strike_rate,
                               ROUND(COALESCE(AVG(CASE WHEN pp.overs_bowled > 0 THEN pp.runs_conceded / pp.overs_bowled END),0),2) AS avg_economy
                        FROM players p
                        LEFT JOIN player_performance pp ON p.player_id = pp.player_id
                        GROUP BY p.player_id, p.name, p.country, p.role, p.batting_style, p.bowling_style
                        ORDER BY total_runs DESC
                        """;
                Statement stmt = con.createStatement();
                ResultSet rs = stmt.executeQuery(sql);
                
                StringBuilder sb = new StringBuilder();
                sb.append("[");
                boolean first = true;
                while (rs.next()) {
                    if (!first) sb.append(",");
                    first = false;
                    sb.append("{\"playerId\":").append(rs.getInt("player_id"))
                      .append(",\"name\":\"").append(rs.getString("name")).append("\"")
                      .append(",\"country\":\"").append(rs.getString("country")).append("\"")
                      .append(",\"role\":\"").append(rs.getString("role")).append("\"")
                      .append(",\"battingStyle\":\"").append(nullToEmpty(rs.getString("batting_style"))).append("\"")
                      .append(",\"bowlingStyle\":\"").append(nullToEmpty(rs.getString("bowling_style"))).append("\"")
                      .append(",\"matchesPlayed\":").append(rs.getInt("matches_played"))
                      .append(",\"totalRuns\":").append(rs.getInt("total_runs"))
                      .append(",\"totalWickets\":").append(rs.getInt("total_wickets"))
                      .append(",\"avgStrikeRate\":").append(rs.getDouble("avg_strike_rate"))
                      .append(",\"avgEconomy\":").append(rs.getDouble("avg_economy"))
                      .append("}");
                }
                sb.append("]");
                ex.getResponseHeaders().add("Content-Type", "application/json");
                sendText(ex, sb.toString());
            } catch (Exception e) {
                System.err.println("PlayersWithStatsHandler error: " + e.getMessage());
                sendText(ex, "[]");
            }
        }
    }

    static class MatchesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            try (Connection con = DatabaseConnection.getConnection()) {
                String sql = """
                        SELECT match_id, match_date, format, tournament, series, season, venue, team1, team2,
                           team1_runs, team1_wickets, team2_runs, team2_wickets,
                           toss_winner, batting_first, batting_second, match_winner
                        FROM matches
                        ORDER BY match_date DESC, match_id DESC
                        """;
                Statement st = con.createStatement();
                ResultSet rs = st.executeQuery(sql);

                StringBuilder sb = new StringBuilder();
                sb.append("[");
                boolean first = true;
                while (rs.next()) {
                    if (!first) sb.append(",");
                    first = false;
                    sb.append("{\"matchId\":").append(rs.getInt("match_id"))
                            .append(",\"date\":\"").append(rs.getDate("match_date")).append("\"")
                            .append(",\"format\":\"").append(rs.getString("format")).append("\"")
                            .append(",\"tournament\":\"").append(nullToEmpty(rs.getString("tournament"))).append("\"")
                            .append(",\"series\":\"").append(nullToEmpty(rs.getString("series"))).append("\"")
                            .append(",\"season\":\"").append(nullToEmpty(rs.getString("season"))).append("\"")
                            .append(",\"venue\":\"").append(nullToEmpty(rs.getString("venue"))).append("\"")
                            .append(",\"team1\":\"").append(nullToEmpty(rs.getString("team1"))).append("\"")
                            .append(",\"team2\":\"").append(nullToEmpty(rs.getString("team2"))).append("\"")
                            .append(",\"team1Runs\":").append(rs.getObject("team1_runs") == null ? "null" : rs.getInt("team1_runs"))
                            .append(",\"team1Wickets\":").append(rs.getObject("team1_wickets") == null ? "null" : rs.getInt("team1_wickets"))
                            .append(",\"team2Runs\":").append(rs.getObject("team2_runs") == null ? "null" : rs.getInt("team2_runs"))
                            .append(",\"team2Wickets\":").append(rs.getObject("team2_wickets") == null ? "null" : rs.getInt("team2_wickets"))
                            .append(",\"tossWinner\":\"").append(nullToEmpty(rs.getString("toss_winner"))).append("\"")
                            .append(",\"battingFirst\":\"").append(nullToEmpty(rs.getString("batting_first"))).append("\"")
                            .append(",\"battingSecond\":\"").append(nullToEmpty(rs.getString("batting_second"))).append("\"")
                            .append(",\"matchWinner\":\"").append(nullToEmpty(rs.getString("match_winner"))).append("\"")
                            .append("}");
                }
                sb.append("]");
                ex.getResponseHeaders().add("Content-Type", "application/json");
                sendText(ex, sb.toString());
            } catch (Exception e) {
                System.err.println("MatchesHandler error: " + e.getMessage());
                sendText(ex, "[]");
            }
        }

        private String nullToEmpty(String value) {
            return value == null ? "" : value.replace("\"", "\\\"");
        }
    }

    static class SearchHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            String q = ex.getRequestURI().getQuery();
            String name = q!=null && q.startsWith("name=")? q.substring(5):"";
            List<Player> records = new PlayerRepository().findAll();
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            boolean first=true;
            for(Player p: records) {
                if(p.getName().toLowerCase().contains(name.toLowerCase())) {
                    if(!first) sb.append(","); first=false;
                    sb.append("{\"playerId\":").append(p.getPlayerId())
                      .append(",\"name\":\"").append(p.getName()).append("\"")
                      .append(",\"country\":\"").append(p.getCountry()).append("\"")
                      .append(",\"role\":\"").append(p.getRole()).append("\"")
                      .append(",\"battingStyle\":\"").append(nullToEmpty(p.getBattingStyle())).append("\"")
                      .append(",\"bowlingStyle\":\"").append(nullToEmpty(p.getBowlingStyle())).append("\"")
                      .append(",\"debutYear\":").append(p.getDebutYear())
                      .append("}");
                }
            }
            sb.append("]");
            ex.getResponseHeaders().add("Content-Type","application/json");
            sendText(ex, sb.toString());
        }
    }

    static class AnalyzeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            String q = ex.getRequestURI().getQuery();
            int id = Integer.parseInt(q.split("=")[1]);
            Player p = new PlayerRepository().findAll().stream()
                    .filter(x -> x.getPlayerId() == id).findFirst().orElse(null);
            if (p == null) {
                ex.getResponseHeaders().add("Content-Type", "application/json");
                sendText(ex, "{\"error\":\"player not found\"}");
                return;
            }
            PerformanceAnalyzer pa = new PerformanceAnalyzer();
            double consistency = pa.calculateConsistency(p);
            double recent = pa.recentForm(p, null);
            double impact = pa.calculateImpact(p, null);
            String role = p.getRole() == null ? "" : p.getRole().trim().toLowerCase().replace('-', '_').replace(' ', '_');
            if (role.equals("allrounder")) role = "all_rounder";

            List<model.PlayerPerformance> perfs = p.getPerformances();
            int totalMatches = perfs == null ? 0 : perfs.size();
            int recentCount = Math.min(totalMatches, 15);
            if (recentCount < 5) recentCount = totalMatches; // min 5

            // Per-match impact details
            StringBuilder matchImpacts = new StringBuilder();
            matchImpacts.append("[");
            if (perfs != null && !perfs.isEmpty()) {
                boolean first = true;
                for (model.PlayerPerformance perf : perfs) {
                    if (!first) matchImpacts.append(",");
                    first = false;
                    double matchImpact = 0;
                    if ("batsman".equals(role)) matchImpact = pa.battingImpactPublic(perf);
                    else if ("bowler".equals(role)) matchImpact = pa.bowlingImpactPublic(perf);
                    else if ("all_rounder".equals(role)) matchImpact = 0.5 * pa.battingImpactPublic(perf) + 0.5 * pa.bowlingImpactPublic(perf);
                    double multiplier = pa.contextMultiplierPublic(perf);
                    String opponent = "";
                    if (perf.getMatch() != null && perf.getTeamName() != null) {
                        String t1 = perf.getMatch().getTeam1() != null ? perf.getMatch().getTeam1() : "";
                        String t2 = perf.getMatch().getTeam2() != null ? perf.getMatch().getTeam2() : "";
                        opponent = perf.getTeamName().equalsIgnoreCase(t1) ? t2 : t1;
                    }
                    String won = "";
                    if (perf.getMatch() != null && perf.getMatch().getMatchWinner() != null && perf.getTeamName() != null) {
                        won = perf.getTeamName().equalsIgnoreCase(perf.getMatch().getMatchWinner()) ? "Won" : "Lost";
                    }
                    matchImpacts.append("{\"matchId\":").append(perf.getMatch() != null ? perf.getMatch().getMatchId() : 0)
                        .append(",\"date\":\"").append(perf.getMatch() != null && perf.getMatch().getDate() != null ? perf.getMatch().getDate().toString() : "").append("\"")
                        .append(",\"format\":\"").append(perf.getMatch() != null && perf.getMatch().getFormat() != null ? perf.getMatch().getFormat() : "").append("\"")
                        .append(",\"opponent\":\"").append(opponent).append("\"")
                        .append(",\"result\":\"").append(won).append("\"")
                        .append(",\"runs\":").append(perf.getRunsScored())
                        .append(",\"balls\":").append(perf.getBallsFaced())
                        .append(",\"wickets\":").append(perf.getWicketsTaken())
                        .append(",\"overs\":").append(String.format("%.1f", perf.getOversBowled()))
                        .append(",\"runsConceded\":").append(perf.getRunsConceded())
                        .append(",\"impactScore\":").append(String.format("%.2f", matchImpact))
                        .append(",\"multiplier\":").append(String.format("%.2f", multiplier))
                        .append("}");
                }
            }
            matchImpacts.append("]");

            // Batting stats
            int totalRuns = perfs == null ? 0 : perfs.stream().mapToInt(model.PlayerPerformance::getRunsScored).sum();
            int totalBalls = perfs == null ? 0 : perfs.stream().mapToInt(model.PlayerPerformance::getBallsFaced).sum();
            double overallSR = totalBalls > 0 ? (totalRuns * 100.0 / totalBalls) : 0;
            double battingAvg = totalMatches > 0 ? ((double)totalRuns / totalMatches) : 0;

            // Bowling stats
            int totalWickets = perfs == null ? 0 : perfs.stream().mapToInt(model.PlayerPerformance::getWicketsTaken).sum();
            double totalOvers = perfs == null ? 0 : perfs.stream().mapToDouble(model.PlayerPerformance::getOversBowled).sum();
            int totalRunsConceded = perfs == null ? 0 : perfs.stream().mapToInt(model.PlayerPerformance::getRunsConceded).sum();
            double avgEconomy = totalOvers > 0 ? (totalRunsConceded / totalOvers) : 0;

            // Format-wise breakdown
            StringBuilder formatBreakdown = new StringBuilder();
            formatBreakdown.append("[");
            if (perfs != null && !perfs.isEmpty()) {
                java.util.Map<String, java.util.List<model.PlayerPerformance>> byFormat = new java.util.LinkedHashMap<>();
                for (model.PlayerPerformance perf : perfs) {
                    String fmt = perf.getMatch() != null && perf.getMatch().getFormat() != null ? perf.getMatch().getFormat() : "Unknown";
                    byFormat.computeIfAbsent(fmt, k -> new java.util.ArrayList<>()).add(perf);
                }
                boolean first = true;
                for (var entry : byFormat.entrySet()) {
                    if (!first) formatBreakdown.append(",");
                    first = false;
                    var fl = entry.getValue();
                    int fMatches = fl.size();
                    int fRuns = fl.stream().mapToInt(model.PlayerPerformance::getRunsScored).sum();
                    int fBalls = fl.stream().mapToInt(model.PlayerPerformance::getBallsFaced).sum();
                    double fSR = fBalls > 0 ? (fRuns * 100.0 / fBalls) : 0;
                    double fAvg = fMatches > 0 ? ((double)fRuns / fMatches) : 0;
                    int fWkts = fl.stream().mapToInt(model.PlayerPerformance::getWicketsTaken).sum();
                    double fOvers = fl.stream().mapToDouble(model.PlayerPerformance::getOversBowled).sum();
                    int fRC = fl.stream().mapToInt(model.PlayerPerformance::getRunsConceded).sum();
                    double fEco = fOvers > 0 ? (fRC / fOvers) : 0;
                    formatBreakdown.append("{\"format\":\"").append(entry.getKey()).append("\"")
                        .append(",\"matches\":").append(fMatches)
                        .append(",\"runs\":").append(fRuns)
                        .append(",\"avg\":").append(String.format("%.2f", fAvg))
                        .append(",\"sr\":").append(String.format("%.2f", fSR))
                        .append(",\"wickets\":").append(fWkts)
                        .append(",\"economy\":").append(String.format("%.2f", fEco))
                        .append("}");
                }
            }
            formatBreakdown.append("]");

            String json = "{\"name\":\"" + p.getName() + "\""
                + ",\"role\":\"" + p.getRole() + "\""
                + ",\"country\":\"" + p.getCountry() + "\""
                + ",\"consistency\":" + String.format("%.2f", consistency)
                + ",\"recentForm\":" + String.format("%.2f", recent)
                + ",\"impactScore\":" + String.format("%.2f", impact)
                + ",\"totalMatches\":" + totalMatches
                + ",\"recentMatchCount\":" + recentCount
                + ",\"totalRuns\":" + totalRuns
                + ",\"totalBalls\":" + totalBalls
                + ",\"overallSR\":" + String.format("%.2f", overallSR)
                + ",\"battingAvg\":" + String.format("%.2f", battingAvg)
                + ",\"totalWickets\":" + totalWickets
                + ",\"totalOvers\":" + String.format("%.1f", totalOvers)
                + ",\"totalRunsConceded\":" + totalRunsConceded
                + ",\"avgEconomy\":" + String.format("%.2f", avgEconomy)
                + ",\"matchImpacts\":" + matchImpacts.toString()
                + ",\"formatBreakdown\":" + formatBreakdown.toString()
                + "}";
            ex.getResponseHeaders().add("Content-Type", "application/json");
            sendText(ex, json);
        }
    }

    static class FilterByYearHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            String yearStr = getQueryParam(ex.getRequestURI().getQuery(), "year");
            if (yearStr == null || yearStr.isEmpty()) {
                sendText(ex, "[]");
                return;
            }

            String sql = """
                    SELECT DISTINCT p.player_id, p.name, p.country, p.role
                    FROM players p
                    JOIN player_performance pp ON pp.player_id = p.player_id
                    JOIN matches m ON m.match_id = pp.match_id
                    WHERE YEAR(m.match_date) = ?
                    ORDER BY p.name
                    """;

            try (Connection con = DatabaseConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, Integer.parseInt(yearStr));
                ResultSet rs = ps.executeQuery();

                StringBuilder sb = new StringBuilder();
                sb.append("[");
                boolean first = true;
                while (rs.next()) {
                    if (!first) sb.append(",");
                    first = false;
                    sb.append("{\"playerId\":").append(rs.getInt("player_id"))
                            .append(",\"name\":\"").append(rs.getString("name")).append("\"")
                            .append(",\"country\":\"").append(rs.getString("country")).append("\"")
                            .append(",\"role\":\"").append(rs.getString("role")).append("\"}");
                }
                sb.append("]");
                ex.getResponseHeaders().add("Content-Type", "application/json");
                sendText(ex, sb.toString());
            } catch (Exception e) {
                System.err.println("FilterByYearHandler error: " + e.getMessage());
                sendText(ex, "[]");
            }
        }
    }

    static class FilterByFormatHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            String format = getQueryParam(ex.getRequestURI().getQuery(), "format");
            if (format == null || format.isEmpty()) {
                sendText(ex, "[]");
                return;
            }

            String sql = """
                    SELECT DISTINCT p.player_id, p.name, p.country, p.role
                    FROM players p
                    JOIN player_performance pp ON pp.player_id = p.player_id
                    JOIN matches m ON m.match_id = pp.match_id
                    WHERE UPPER(m.format) = UPPER(?)
                    ORDER BY p.name
                    """;

            try (Connection con = DatabaseConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, format.trim());
                ResultSet rs = ps.executeQuery();

                StringBuilder sb = new StringBuilder();
                sb.append("[");
                boolean first = true;
                while (rs.next()) {
                    if (!first) sb.append(",");
                    first = false;
                    sb.append("{\"playerId\":").append(rs.getInt("player_id"))
                            .append(",\"name\":\"").append(rs.getString("name")).append("\"")
                            .append(",\"country\":\"").append(rs.getString("country")).append("\"")
                            .append(",\"role\":\"").append(rs.getString("role")).append("\"}");
                }
                sb.append("]");
                ex.getResponseHeaders().add("Content-Type", "application/json");
                sendText(ex, sb.toString());
            } catch (Exception e) {
                System.err.println("FilterByFormatHandler error: " + e.getMessage());
                sendText(ex, "[]");
            }
        }
    }

    static class FilterByTournamentHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            String tournament = getQueryParam(ex.getRequestURI().getQuery(), "tournament");
            if (tournament == null || tournament.isEmpty()) {
                sendText(ex, "[]");
                return;
            }

            String sql = """
                    SELECT DISTINCT p.player_id, p.name, p.country, p.role
                    FROM players p
                    JOIN player_performance pp ON pp.player_id = p.player_id
                    JOIN matches m ON m.match_id = pp.match_id
                    WHERE LOWER(m.tournament) LIKE LOWER(?)
                    ORDER BY p.name
                    """;

            try (Connection con = DatabaseConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, "%" + tournament.trim() + "%");
                ResultSet rs = ps.executeQuery();

                StringBuilder sb = new StringBuilder();
                sb.append("[");
                boolean first = true;
                while (rs.next()) {
                    if (!first) sb.append(",");
                    first = false;
                    sb.append("{\"playerId\":").append(rs.getInt("player_id"))
                            .append(",\"name\":\"").append(rs.getString("name")).append("\"")
                            .append(",\"country\":\"").append(rs.getString("country")).append("\"")
                            .append(",\"role\":\"").append(rs.getString("role")).append("\"}");
                }
                sb.append("]");
                ex.getResponseHeaders().add("Content-Type", "application/json");
                sendText(ex, sb.toString());
            } catch (Exception e) {
                System.err.println("FilterByTournamentHandler error: " + e.getMessage());
                sendText(ex, "[]");
            }
        }
    }

    static class FilterByCountryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            String country = getQueryParam(ex.getRequestURI().getQuery(), "country");
            if (country == null || country.isEmpty()) {
                sendText(ex, "[]");
                return;
            }

            String sql = """
                    SELECT DISTINCT player_id, name, country, role
                    FROM players
                    WHERE LOWER(country) LIKE LOWER(?)
                    ORDER BY name
                    """;

            try (Connection con = DatabaseConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, "%" + country.trim() + "%");
                ResultSet rs = ps.executeQuery();

                StringBuilder sb = new StringBuilder();
                sb.append("[");
                boolean first = true;
                while (rs.next()) {
                    if (!first) sb.append(",");
                    first = false;
                    sb.append("{\"playerId\":").append(rs.getInt("player_id"))
                            .append(",\"name\":\"").append(rs.getString("name")).append("\"")
                            .append(",\"country\":\"").append(rs.getString("country")).append("\"")
                            .append(",\"role\":\"").append(rs.getString("role")).append("\"}");
                }
                sb.append("]");
                ex.getResponseHeaders().add("Content-Type", "application/json");
                sendText(ex, sb.toString());
            } catch (Exception e) {
                System.err.println("FilterByCountryHandler error: " + e.getMessage());
                sendText(ex, "[]");
            }
        }
    }

    static class FilterByRoleHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            String role = getQueryParam(ex.getRequestURI().getQuery(), "role");
            if (role == null || role.isEmpty()) {
                sendText(ex, "[]");
                return;
            }

            String sql = """
                    SELECT DISTINCT player_id, name, country, role
                    FROM players
                    WHERE LOWER(role) LIKE LOWER(?)
                    ORDER BY name
                    """;

            try (Connection con = DatabaseConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, "%" + role.trim() + "%");
                ResultSet rs = ps.executeQuery();

                StringBuilder sb = new StringBuilder();
                sb.append("[");
                boolean first = true;
                while (rs.next()) {
                    if (!first) sb.append(",");
                    first = false;
                    sb.append("{\"playerId\":").append(rs.getInt("player_id"))
                            .append(",\"name\":\"").append(rs.getString("name")).append("\"")
                            .append(",\"country\":\"").append(rs.getString("country")).append("\"")
                            .append(",\"role\":\"").append(rs.getString("role")).append("\"}");
                }
                sb.append("]");
                ex.getResponseHeaders().add("Content-Type", "application/json");
                sendText(ex, sb.toString());
            } catch (Exception e) {
                System.err.println("FilterByRoleHandler error: " + e.getMessage());
                sendText(ex, "[]");
            }
        }
    }

    static class CompareHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            String q = ex.getRequestURI().getQuery();
            String[] parts = q.split("&");
            int id1 = Integer.parseInt(parts[0].substring(parts[0].indexOf('=')+1));
            int id2 = Integer.parseInt(parts[1].substring(parts[1].indexOf('=')+1));
            List<Player> list = new PlayerRepository().findAll();
            Player p1 = list.stream().filter(x -> x.getPlayerId() == id1).findFirst().orElse(null);
            Player p2 = list.stream().filter(x -> x.getPlayerId() == id2).findFirst().orElse(null);

            if (p1 == null || p2 == null) {
                sendJson(ex, "{\"error\":\"One or both players not found\"}");
                return;
            }

            String role1 = normalizeRoleStatic(p1.getRole());
            String role2 = normalizeRoleStatic(p2.getRole());

            // Determine comparison type
            String compType;
            if (role1.equals(role2)) {
                compType = role1; // batsman, bowler, or all_rounder
            } else {
                sendJson(ex, "{\"error\":\"Cannot compare different roles: " + p1.getRole() + " vs " + p2.getRole() + ". Please compare players of the same role.\"}");
                return;
            }

            PerformanceAnalyzer analyzer = new PerformanceAnalyzer();

            StringBuilder sb = new StringBuilder("{");
            sb.append("\"error\":null,");
            sb.append("\"comparisonType\":\"").append(compType).append("\",");

            // Build player stats for both
            sb.append("\"player1\":").append(buildPlayerCompareJson(p1, analyzer)).append(",");
            sb.append("\"player2\":").append(buildPlayerCompareJson(p2, analyzer)).append(",");

            // Determine verdict with scoring system
            double score1 = 0, score2 = 0;
            double c1 = analyzer.calculateConsistency(p1);
            double c2 = analyzer.calculateConsistency(p2);
            double r1 = analyzer.recentForm(p1, null);
            double r2 = analyzer.recentForm(p2, null);
            double i1 = analyzer.calculateImpact(p1, null);
            double i2 = analyzer.calculateImpact(p2, null);

            if (compType.equals("batsman")) {
                int runs1 = p1.getPerformances().stream().mapToInt(PlayerPerformance::getRunsScored).sum();
                int runs2 = p2.getPerformances().stream().mapToInt(PlayerPerformance::getRunsScored).sum();
                double sr1 = calcOverallSR(p1); double sr2 = calcOverallSR(p2);
                double avg1 = calcBattingAvg(p1); double avg2 = calcBattingAvg(p2);
                if (runs1 > runs2) score1 += 1; else if (runs2 > runs1) score2 += 1;
                if (sr1 > sr2) score1 += 1; else if (sr2 > sr1) score2 += 1;
                if (avg1 > avg2) score1 += 1; else if (avg2 > avg1) score2 += 1;
            } else if (compType.equals("bowler")) {
                int wkts1 = p1.getPerformances().stream().mapToInt(PlayerPerformance::getWicketsTaken).sum();
                int wkts2 = p2.getPerformances().stream().mapToInt(PlayerPerformance::getWicketsTaken).sum();
                double eco1 = calcAvgEconomy(p1); double eco2 = calcAvgEconomy(p2);
                if (wkts1 > wkts2) score1 += 1; else if (wkts2 > wkts1) score2 += 1;
                if (eco1 < eco2) score1 += 1; else if (eco2 < eco1) score2 += 1;
            } else { // all_rounder
                int runs1 = p1.getPerformances().stream().mapToInt(PlayerPerformance::getRunsScored).sum();
                int runs2 = p2.getPerformances().stream().mapToInt(PlayerPerformance::getRunsScored).sum();
                int wkts1 = p1.getPerformances().stream().mapToInt(PlayerPerformance::getWicketsTaken).sum();
                int wkts2 = p2.getPerformances().stream().mapToInt(PlayerPerformance::getWicketsTaken).sum();
                if (runs1 > runs2) score1 += 1; else if (runs2 > runs1) score2 += 1;
                if (wkts1 > wkts2) score1 += 1; else if (wkts2 > wkts1) score2 += 1;
            }
            // Common metrics
            if (c1 > c2) score1 += 1; else if (c2 > c1) score2 += 1;
            if (r1 > r2) score1 += 1; else if (r2 > r1) score2 += 1;
            if (i1 > i2) score1 += 1; else if (i2 > i1) score2 += 1;

            String verdict = score1 > score2 ? p1.getName() : score2 > score1 ? p2.getName() : "Tie";
            sb.append("\"verdictName\":\"").append(verdict).append("\",");
            sb.append("\"verdictScore1\":").append(score1).append(",");
            sb.append("\"verdictScore2\":").append(score2);
            sb.append("}");

            sendJson(ex, sb.toString());
        }

        private String buildPlayerCompareJson(Player p, PerformanceAnalyzer analyzer) {
            java.util.List<PlayerPerformance> perfs = p.getPerformances();
            int matches = perfs != null ? perfs.size() : 0;
            int totalRuns = perfs != null ? perfs.stream().mapToInt(PlayerPerformance::getRunsScored).sum() : 0;
            int totalBalls = perfs != null ? perfs.stream().mapToInt(PlayerPerformance::getBallsFaced).sum() : 0;
            int totalWickets = perfs != null ? perfs.stream().mapToInt(PlayerPerformance::getWicketsTaken).sum() : 0;
            double totalOvers = perfs != null ? perfs.stream().mapToDouble(PlayerPerformance::getOversBowled).sum() : 0;
            int totalRunsConceded = perfs != null ? perfs.stream().mapToInt(PlayerPerformance::getRunsConceded).sum() : 0;
            double overallSR = totalBalls > 0 ? (totalRuns * 100.0 / totalBalls) : 0;
            double battingAvg = matches > 0 ? (double) totalRuns / matches : 0;
            double avgEconomy = totalOvers > 0 ? totalRunsConceded / totalOvers : 0;
            double consistency = analyzer.calculateConsistency(p);
            double recentForm = analyzer.recentForm(p, null);
            double impactScore = analyzer.calculateImpact(p, null);

            // Milestones
            int fifties = 0, hundreds = 0, highestScore = 0, bestBowling = 0;
            int winsCount = 0;
            if (perfs != null) {
                for (PlayerPerformance pp : perfs) {
                    if (pp.getRunsScored() >= 100) hundreds++;
                    else if (pp.getRunsScored() >= 50) fifties++;
                    if (pp.getRunsScored() > highestScore) highestScore = pp.getRunsScored();
                    if (pp.getWicketsTaken() > bestBowling) bestBowling = pp.getWicketsTaken();
                    if (pp.getTeamName() != null && pp.getMatch() != null &&
                        pp.getMatch().getMatchWinner() != null &&
                        pp.getTeamName().trim().equalsIgnoreCase(pp.getMatch().getMatchWinner().trim())) {
                        winsCount++;
                    }
                }
            }
            double winRate = matches > 0 ? (winsCount * 100.0 / matches) : 0;

            // Format breakdown
            java.util.Map<String, java.util.List<PlayerPerformance>> formatMap = new java.util.LinkedHashMap<>();
            if (perfs != null) {
                for (PlayerPerformance pp : perfs) {
                    String fmt = pp.getMatch() != null && pp.getMatch().getFormat() != null ? pp.getMatch().getFormat() : "Unknown";
                    formatMap.computeIfAbsent(fmt, k -> new java.util.ArrayList<>()).add(pp);
                }
            }

            StringBuilder fb = new StringBuilder("[");
            boolean firstFmt = true;
            for (var entry : formatMap.entrySet()) {
                if (!firstFmt) fb.append(",");
                firstFmt = false;
                java.util.List<PlayerPerformance> fPerfs = entry.getValue();
                int fMatches = fPerfs.size();
                int fRuns = fPerfs.stream().mapToInt(PlayerPerformance::getRunsScored).sum();
                int fBalls = fPerfs.stream().mapToInt(PlayerPerformance::getBallsFaced).sum();
                int fWkts = fPerfs.stream().mapToInt(PlayerPerformance::getWicketsTaken).sum();
                double fOvers = fPerfs.stream().mapToDouble(PlayerPerformance::getOversBowled).sum();
                int fConc = fPerfs.stream().mapToInt(PlayerPerformance::getRunsConceded).sum();
                double fSR = fBalls > 0 ? fRuns * 100.0 / fBalls : 0;
                double fAvg = fMatches > 0 ? (double)fRuns / fMatches : 0;
                double fEco = fOvers > 0 ? fConc / fOvers : 0;
                fb.append(String.format("{\"format\":\"%s\",\"matches\":%d,\"runs\":%d,\"avg\":\"%.1f\",\"sr\":\"%.1f\",\"wickets\":%d,\"economy\":\"%.2f\"}",
                        entry.getKey(), fMatches, fRuns, fAvg, fSR, fWkts, fEco));
            }
            fb.append("]");

            return String.format("{\"name\":\"%s\",\"country\":\"%s\",\"role\":\"%s\",\"matches\":%d," +
                "\"totalRuns\":%d,\"totalBalls\":%d,\"battingAvg\":%.1f,\"strikeRate\":%.1f," +
                "\"totalWickets\":%d,\"totalOvers\":%.1f,\"totalRunsConceded\":%d,\"avgEconomy\":%.2f," +
                "\"consistency\":%.2f,\"recentForm\":%.2f,\"impactScore\":%.2f," +
                "\"highestScore\":%d,\"bestBowling\":%d,\"fifties\":%d,\"hundreds\":%d," +
                "\"winRate\":%.1f,\"winsCount\":%d,\"formatBreakdown\":%s}",
                p.getName(), p.getCountry(), p.getRole(), matches,
                totalRuns, totalBalls, battingAvg, overallSR,
                totalWickets, totalOvers, totalRunsConceded, avgEconomy,
                consistency, recentForm, impactScore,
                highestScore, bestBowling, fifties, hundreds,
                winRate, winsCount, fb.toString());
        }

        private double calcOverallSR(Player p) {
            int runs = p.getPerformances().stream().mapToInt(PlayerPerformance::getRunsScored).sum();
            int balls = p.getPerformances().stream().mapToInt(PlayerPerformance::getBallsFaced).sum();
            return balls > 0 ? runs * 100.0 / balls : 0;
        }
        private double calcBattingAvg(Player p) {
            int runs = p.getPerformances().stream().mapToInt(PlayerPerformance::getRunsScored).sum();
            int matches = p.getPerformances().size();
            return matches > 0 ? (double)runs / matches : 0;
        }
        private double calcAvgEconomy(Player p) {
            double overs = p.getPerformances().stream().mapToDouble(PlayerPerformance::getOversBowled).sum();
            int conc = p.getPerformances().stream().mapToInt(PlayerPerformance::getRunsConceded).sum();
            return overs > 0 ? conc / overs : 0;
        }

        private static String normalizeRoleStatic(String role) {
            if (role == null) return "";
            String n = role.trim().toLowerCase().replace('-','_').replace(' ','_');
            if (n.equals("allrounder")) return "all_rounder";
            return n;
        }
    }

    // ===================== GENERATE REPORT =====================
    static class ReportHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            String idStr = getQueryParam(ex.getRequestURI().getQuery(), "id");
            if (idStr == null || idStr.isBlank()) {
                sendJson(ex, "{\"error\":\"Missing player id\"}");
                return;
            }
            try (Connection conn = DatabaseConnection.getConnection()) {
                int pid = Integer.parseInt(idStr);

                // Player info
                PreparedStatement ps = conn.prepareStatement("SELECT name, country, role, debut_year, batting_style, bowling_style FROM players WHERE player_id = ?");
                ps.setInt(1, pid);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) { sendJson(ex, "{\"error\":\"Player not found\"}"); return; }
                String name = rs.getString("name");
                String country = rs.getString("country");
                String role = rs.getString("role");
                int debutYear = rs.getInt("debut_year");
                String battingStyle = rs.getString("batting_style");
                String bowlingStyle = rs.getString("bowling_style");
                rs.close(); ps.close();

                // Career totals
                ps = conn.prepareStatement(
                    "SELECT COUNT(DISTINCT pp.performance_id) AS mat, " +
                    "COALESCE(SUM(pp.runs_scored),0) AS runs, COALESCE(SUM(pp.balls_faced),0) AS balls, " +
                    "COALESCE(SUM(pp.wickets_taken),0) AS wkts, COALESCE(SUM(pp.overs_bowled),0) AS overs, " +
                    "COALESCE(SUM(pp.runs_conceded),0) AS rc, COALESCE(SUM(pp.fours),0) AS fours, " +
                    "COALESCE(SUM(pp.sixes),0) AS sixes, COALESCE(SUM(pp.catches),0) AS catches, " +
                    "COALESCE(SUM(pp.maidens),0) AS maidens, " +
                    "COALESCE(MAX(pp.runs_scored),0) AS highest_score, " +
                    "ROUND(CASE WHEN COUNT(pp.performance_id)>0 THEN SUM(pp.runs_scored)*1.0/COUNT(pp.performance_id) ELSE 0 END,2) AS bat_avg, " +
                    "ROUND(CASE WHEN SUM(pp.balls_faced)>0 THEN SUM(pp.runs_scored)*100.0/SUM(pp.balls_faced) ELSE 0 END,2) AS sr, " +
                    "ROUND(CASE WHEN SUM(pp.overs_bowled)>0 THEN SUM(pp.runs_conceded)*1.0/SUM(pp.overs_bowled) ELSE 0 END,2) AS eco, " +
                    "ROUND(CASE WHEN SUM(pp.wickets_taken)>0 THEN SUM(pp.runs_conceded)*1.0/SUM(pp.wickets_taken) ELSE 0 END,2) AS bowl_avg " +
                    "FROM player_performance pp WHERE pp.player_id = ?");
                ps.setInt(1, pid);
                rs = ps.executeQuery();
                rs.next();
                int totalMat = rs.getInt("mat"); int totalRuns = rs.getInt("runs"); int totalBalls = rs.getInt("balls");
                int totalWkts = rs.getInt("wkts"); double totalOvers = rs.getDouble("overs"); int totalRC = rs.getInt("rc");
                int totalFours = rs.getInt("fours"); int totalSixes = rs.getInt("sixes");
                int totalCatches = rs.getInt("catches"); int totalMaidens = rs.getInt("maidens");
                int highestScore = rs.getInt("highest_score");
                double batAvg = rs.getDouble("bat_avg"); double sr = rs.getDouble("sr");
                double eco = rs.getDouble("eco"); double bowlAvg = rs.getDouble("bowl_avg");
                rs.close(); ps.close();

                // Format-wise breakdown
                ps = conn.prepareStatement(
                    "SELECT m.format, COUNT(DISTINCT pp.performance_id) AS mat, " +
                    "COALESCE(SUM(pp.runs_scored),0) AS runs, COALESCE(SUM(pp.balls_faced),0) AS balls, " +
                    "COALESCE(SUM(pp.wickets_taken),0) AS wkts, COALESCE(SUM(pp.overs_bowled),0) AS overs, " +
                    "COALESCE(SUM(pp.runs_conceded),0) AS rc, COALESCE(SUM(pp.fours),0) AS fours, " +
                    "COALESCE(SUM(pp.sixes),0) AS sixes, COALESCE(MAX(pp.runs_scored),0) AS hs, " +
                    "ROUND(CASE WHEN COUNT(pp.performance_id)>0 THEN SUM(pp.runs_scored)*1.0/COUNT(pp.performance_id) ELSE 0 END,2) AS avg, " +
                    "ROUND(CASE WHEN SUM(pp.balls_faced)>0 THEN SUM(pp.runs_scored)*100.0/SUM(pp.balls_faced) ELSE 0 END,2) AS sr, " +
                    "ROUND(CASE WHEN SUM(pp.overs_bowled)>0 THEN SUM(pp.runs_conceded)*1.0/SUM(pp.overs_bowled) ELSE 0 END,2) AS eco " +
                    "FROM player_performance pp JOIN matches m ON pp.match_id=m.match_id " +
                    "WHERE pp.player_id=? GROUP BY m.format ORDER BY m.format");
                ps.setInt(1, pid);
                rs = ps.executeQuery();
                StringBuilder fmtData = new StringBuilder("[");
                boolean first = true;
                while (rs.next()) {
                    if (!first) fmtData.append(","); first = false;
                    fmtData.append(String.format("{\"format\":\"%s\",\"matches\":%d,\"runs\":%d,\"balls\":%d,\"wickets\":%d,\"fours\":%d,\"sixes\":%d,\"highScore\":%d,\"avg\":%.2f,\"sr\":%.2f,\"economy\":%.2f}",
                        rs.getString("format"), rs.getInt("mat"), rs.getInt("runs"), rs.getInt("balls"),
                        rs.getInt("wkts"), rs.getInt("fours"), rs.getInt("sixes"), rs.getInt("hs"),
                        rs.getDouble("avg"), rs.getDouble("sr"), rs.getDouble("eco")));
                }
                fmtData.append("]");
                rs.close(); ps.close();

                // Season/Year-wise breakdown (ties to Season class)
                ps = conn.prepareStatement(
                    "SELECT YEAR(m.match_date) AS yr, COALESCE(m.season,'') AS season_name, " +
                    "COUNT(DISTINCT pp.performance_id) AS mat, " +
                    "COALESCE(SUM(pp.runs_scored),0) AS runs, COALESCE(SUM(pp.balls_faced),0) AS balls, " +
                    "COALESCE(SUM(pp.wickets_taken),0) AS wkts, COALESCE(SUM(pp.overs_bowled),0) AS overs, " +
                    "COALESCE(SUM(pp.runs_conceded),0) AS rc, " +
                    "ROUND(CASE WHEN COUNT(pp.performance_id)>0 THEN SUM(pp.runs_scored)*1.0/COUNT(pp.performance_id) ELSE 0 END,2) AS avg, " +
                    "ROUND(CASE WHEN SUM(pp.balls_faced)>0 THEN SUM(pp.runs_scored)*100.0/SUM(pp.balls_faced) ELSE 0 END,2) AS sr, " +
                    "ROUND(CASE WHEN SUM(pp.overs_bowled)>0 THEN SUM(pp.runs_conceded)*1.0/SUM(pp.overs_bowled) ELSE 0 END,2) AS eco " +
                    "FROM player_performance pp JOIN matches m ON pp.match_id=m.match_id " +
                    "WHERE pp.player_id=? GROUP BY YEAR(m.match_date), m.season ORDER BY yr");
                ps.setInt(1, pid);
                rs = ps.executeQuery();
                StringBuilder seasonData = new StringBuilder("[");
                first = true;
                while (rs.next()) {
                    if (!first) seasonData.append(","); first = false;
                    String seasonName = rs.getString("season_name");
                    if (seasonName == null) seasonName = "";
                    seasonData.append(String.format("{\"year\":%d,\"season\":\"%s\",\"matches\":%d,\"runs\":%d,\"wickets\":%d,\"avg\":%.2f,\"sr\":%.2f,\"economy\":%.2f}",
                        rs.getInt("yr"), seasonName.replace("\"","\\\""), rs.getInt("mat"), rs.getInt("runs"),
                        rs.getInt("wkts"), rs.getDouble("avg"), rs.getDouble("sr"), rs.getDouble("eco")));
                }
                seasonData.append("]");
                rs.close(); ps.close();

                // Best performances (top 5 batting innings)
                ps = conn.prepareStatement(
                    "SELECT pp.runs_scored, pp.balls_faced, pp.wickets_taken, pp.fours, pp.sixes, " +
                    "m.format, m.match_date, CONCAT(m.team1,' vs ',m.team2) AS matchup, m.tournament " +
                    "FROM player_performance pp JOIN matches m ON pp.match_id=m.match_id " +
                    "WHERE pp.player_id=? ORDER BY pp.runs_scored DESC LIMIT 5");
                ps.setInt(1, pid);
                rs = ps.executeQuery();
                StringBuilder bestBat = new StringBuilder("[");
                first = true;
                while (rs.next()) {
                    if (!first) bestBat.append(","); first = false;
                    String tour = rs.getString("tournament");
                    if (tour == null) tour = "";
                    bestBat.append(String.format("{\"runs\":%d,\"balls\":%d,\"fours\":%d,\"sixes\":%d,\"format\":\"%s\",\"date\":\"%s\",\"match\":\"%s\",\"tournament\":\"%s\"}",
                        rs.getInt("runs_scored"), rs.getInt("balls_faced"), rs.getInt("fours"), rs.getInt("sixes"),
                        rs.getString("format"), rs.getDate("match_date"),
                        rs.getString("matchup").replace("\"","\\\""), tour.replace("\"","\\\"")));
                }
                bestBat.append("]");
                rs.close(); ps.close();

                // Best bowling figures (top 5)
                ps = conn.prepareStatement(
                    "SELECT pp.wickets_taken, pp.runs_conceded, pp.overs_bowled, pp.maidens, " +
                    "m.format, m.match_date, CONCAT(m.team1,' vs ',m.team2) AS matchup, m.tournament " +
                    "FROM player_performance pp JOIN matches m ON pp.match_id=m.match_id " +
                    "WHERE pp.player_id=? AND pp.wickets_taken > 0 ORDER BY pp.wickets_taken DESC, pp.runs_conceded ASC LIMIT 5");
                ps.setInt(1, pid);
                rs = ps.executeQuery();
                StringBuilder bestBowl = new StringBuilder("[");
                first = true;
                while (rs.next()) {
                    if (!first) bestBowl.append(","); first = false;
                    String tour = rs.getString("tournament");
                    if (tour == null) tour = "";
                    bestBowl.append(String.format("{\"wickets\":%d,\"runs\":%d,\"overs\":%.1f,\"maidens\":%d,\"format\":\"%s\",\"date\":\"%s\",\"match\":\"%s\",\"tournament\":\"%s\"}",
                        rs.getInt("wickets_taken"), rs.getInt("runs_conceded"), rs.getDouble("overs_bowled"),
                        rs.getInt("maidens"), rs.getString("format"), rs.getDate("match_date"),
                        rs.getString("matchup").replace("\"","\\\""), tour.replace("\"","\\\"")));
                }
                bestBowl.append("]");
                rs.close(); ps.close();

                // Build final JSON
                String json = String.format(
                    "{\"name\":\"%s\",\"country\":\"%s\",\"role\":\"%s\",\"debutYear\":%d," +
                    "\"battingStyle\":\"%s\",\"bowlingStyle\":\"%s\"," +
                    "\"career\":{\"matches\":%d,\"runs\":%d,\"balls\":%d,\"wickets\":%d,\"overs\":%.1f," +
                    "\"runsConceded\":%d,\"fours\":%d,\"sixes\":%d,\"catches\":%d,\"maidens\":%d," +
                    "\"highestScore\":%d,\"battingAvg\":%.2f,\"strikeRate\":%.2f,\"economy\":%.2f,\"bowlingAvg\":%.2f}," +
                    "\"formatBreakdown\":%s,\"seasonBreakdown\":%s,\"bestBatting\":%s,\"bestBowling\":%s}",
                    name.replace("\"","\\\""), country.replace("\"","\\\""), role.replace("\"","\\\""), debutYear,
                    nullToEmpty(battingStyle).replace("\"","\\\""), nullToEmpty(bowlingStyle).replace("\"","\\\""),
                    totalMat, totalRuns, totalBalls, totalWkts, totalOvers, totalRC,
                    totalFours, totalSixes, totalCatches, totalMaidens, highestScore,
                    batAvg, sr, eco, bowlAvg,
                    fmtData, seasonData, bestBat, bestBowl);
                sendJson(ex, json);
            } catch (Exception e) {
                System.err.println("ReportHandler error: " + e.getMessage());
                e.printStackTrace();
                sendJson(ex, "{\"error\":\"Error generating report\"}");
            }
        }
    }

    // ===================== SEASON REPORT (generateSeasonReport) =====================
    static class SeasonReportHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            String yearStr = getQueryParam(ex.getRequestURI().getQuery(), "year");
            if (yearStr == null || yearStr.isBlank()) {
                // Return list of available years/seasons
                try (Connection conn = DatabaseConnection.getConnection()) {
                    PreparedStatement ps = conn.prepareStatement(
                        "SELECT DISTINCT YEAR(match_date) AS yr FROM matches WHERE match_date IS NOT NULL ORDER BY yr DESC");
                    ResultSet rs = ps.executeQuery();
                    StringBuilder sb = new StringBuilder("{\"years\":[");
                    boolean first = true;
                    while (rs.next()) {
                        if (!first) sb.append(","); first = false;
                        sb.append(rs.getInt("yr"));
                    }
                    sb.append("]}");
                    rs.close(); ps.close();
                    sendJson(ex, sb.toString());
                } catch (Exception e) {
                    sendJson(ex, "{\"error\":\"Error fetching years\"}");
                }
                return;
            }
            try (Connection conn = DatabaseConnection.getConnection()) {
                int year = Integer.parseInt(yearStr);

                // Season summary: total matches, runs, wickets by format
                PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(DISTINCT m.match_id) AS total_matches, " +
                    "m.format, COALESCE(m.season,'') AS season_name, " +
                    "COALESCE(SUM(pp.runs_scored),0) AS total_runs, " +
                    "COALESCE(SUM(pp.wickets_taken),0) AS total_wickets, " +
                    "COALESCE(SUM(pp.sixes),0) AS total_sixes, " +
                    "COALESCE(SUM(pp.fours),0) AS total_fours, " +
                    "COUNT(DISTINCT pp.player_id) AS players_count " +
                    "FROM matches m LEFT JOIN player_performance pp ON m.match_id=pp.match_id " +
                    "WHERE YEAR(m.match_date)=? GROUP BY m.format, m.season ORDER BY m.format");
                ps.setInt(1, year);
                ResultSet rs = ps.executeQuery();

                int grandMatches = 0, grandRuns = 0, grandWkts = 0, grandSixes = 0, grandFours = 0;
                StringBuilder fmtArr = new StringBuilder("[");
                boolean first = true;
                while (rs.next()) {
                    if (!first) fmtArr.append(","); first = false;
                    int mat = rs.getInt("total_matches");
                    int runs = rs.getInt("total_runs");
                    int wkts = rs.getInt("total_wickets");
                    int sixes = rs.getInt("total_sixes");
                    int fours = rs.getInt("total_fours");
                    grandMatches += mat; grandRuns += runs; grandWkts += wkts; grandSixes += sixes; grandFours += fours;
                    String sn = rs.getString("season_name");
                    if (sn == null) sn = "";
                    fmtArr.append(String.format("{\"format\":\"%s\",\"season\":\"%s\",\"matches\":%d,\"runs\":%d,\"wickets\":%d,\"sixes\":%d,\"fours\":%d,\"players\":%d}",
                        rs.getString("format"), sn.replace("\"","\\\""), mat, runs, wkts, sixes, fours, rs.getInt("players_count")));
                }
                fmtArr.append("]");
                rs.close(); ps.close();

                // Top run scorers
                ps = conn.prepareStatement(
                    "SELECT p.player_id, p.name, p.country, p.role, " +
                    "SUM(pp.runs_scored) AS total_runs, COUNT(pp.performance_id) AS mat, " +
                    "SUM(pp.balls_faced) AS balls, SUM(pp.fours) AS fours, SUM(pp.sixes) AS sixes, " +
                    "MAX(pp.runs_scored) AS hs, " +
                    "ROUND(CASE WHEN COUNT(pp.performance_id)>0 THEN SUM(pp.runs_scored)*1.0/COUNT(pp.performance_id) ELSE 0 END,2) AS avg, " +
                    "ROUND(CASE WHEN SUM(pp.balls_faced)>0 THEN SUM(pp.runs_scored)*100.0/SUM(pp.balls_faced) ELSE 0 END,2) AS sr " +
                    "FROM player_performance pp JOIN matches m ON pp.match_id=m.match_id " +
                    "JOIN players p ON pp.player_id=p.player_id " +
                    "WHERE YEAR(m.match_date)=? GROUP BY p.player_id ORDER BY total_runs DESC LIMIT 10");
                ps.setInt(1, year);
                rs = ps.executeQuery();
                StringBuilder topBat = new StringBuilder("[");
                first = true;
                while (rs.next()) {
                    if (!first) topBat.append(","); first = false;
                    topBat.append(String.format("{\"playerId\":%d,\"name\":\"%s\",\"country\":\"%s\",\"role\":\"%s\",\"runs\":%d,\"matches\":%d,\"balls\":%d,\"fours\":%d,\"sixes\":%d,\"highScore\":%d,\"avg\":%.2f,\"sr\":%.2f}",
                        rs.getInt("player_id"), rs.getString("name").replace("\"","\\\""),
                        rs.getString("country").replace("\"","\\\""), rs.getString("role"),
                        rs.getInt("total_runs"), rs.getInt("mat"), rs.getInt("balls"),
                        rs.getInt("fours"), rs.getInt("sixes"), rs.getInt("hs"),
                        rs.getDouble("avg"), rs.getDouble("sr")));
                }
                topBat.append("]");
                rs.close(); ps.close();

                // Top wicket takers
                ps = conn.prepareStatement(
                    "SELECT p.player_id, p.name, p.country, p.role, " +
                    "SUM(pp.wickets_taken) AS total_wickets, COUNT(pp.performance_id) AS mat, " +
                    "SUM(pp.overs_bowled) AS overs, SUM(pp.runs_conceded) AS rc, SUM(pp.maidens) AS maidens, " +
                    "MAX(pp.wickets_taken) AS best_wkts, " +
                    "ROUND(CASE WHEN SUM(pp.overs_bowled)>0 THEN SUM(pp.runs_conceded)*1.0/SUM(pp.overs_bowled) ELSE 0 END,2) AS eco, " +
                    "ROUND(CASE WHEN SUM(pp.wickets_taken)>0 THEN SUM(pp.runs_conceded)*1.0/SUM(pp.wickets_taken) ELSE 0 END,2) AS bowl_avg " +
                    "FROM player_performance pp JOIN matches m ON pp.match_id=m.match_id " +
                    "JOIN players p ON pp.player_id=p.player_id " +
                    "WHERE YEAR(m.match_date)=? AND pp.wickets_taken>0 GROUP BY p.player_id ORDER BY total_wickets DESC LIMIT 10");
                ps.setInt(1, year);
                rs = ps.executeQuery();
                StringBuilder topBowl = new StringBuilder("[");
                first = true;
                while (rs.next()) {
                    if (!first) topBowl.append(","); first = false;
                    topBowl.append(String.format("{\"playerId\":%d,\"name\":\"%s\",\"country\":\"%s\",\"role\":\"%s\",\"wickets\":%d,\"matches\":%d,\"overs\":%.1f,\"runsConceded\":%d,\"maidens\":%d,\"bestWickets\":%d,\"economy\":%.2f,\"bowlingAvg\":%.2f}",
                        rs.getInt("player_id"), rs.getString("name").replace("\"","\\\""),
                        rs.getString("country").replace("\"","\\\""), rs.getString("role"),
                        rs.getInt("total_wickets"), rs.getInt("mat"), rs.getDouble("overs"),
                        rs.getInt("rc"), rs.getInt("maidens"), rs.getInt("best_wkts"),
                        rs.getDouble("eco"), rs.getDouble("bowl_avg")));
                }
                topBowl.append("]");
                rs.close(); ps.close();

                // Top match results in the season
                ps = conn.prepareStatement(
                    "SELECT match_id, format, tournament, season, venue, match_date, team1, team2, " +
                    "team1_runs, team1_wickets, team2_runs, team2_wickets, match_winner " +
                    "FROM matches WHERE YEAR(match_date)=? ORDER BY match_date DESC LIMIT 20");
                ps.setInt(1, year);
                rs = ps.executeQuery();
                StringBuilder matchList = new StringBuilder("[");
                first = true;
                while (rs.next()) {
                    if (!first) matchList.append(","); first = false;
                    String tour = rs.getString("tournament"); if (tour == null) tour = "";
                    String ven = rs.getString("venue"); if (ven == null) ven = "";
                    String winner = rs.getString("match_winner"); if (winner == null) winner = "";
                    matchList.append(String.format("{\"matchId\":%d,\"format\":\"%s\",\"tournament\":\"%s\",\"venue\":\"%s\",\"date\":\"%s\",\"team1\":\"%s\",\"team2\":\"%s\",\"t1Runs\":%s,\"t1Wkts\":%s,\"t2Runs\":%s,\"t2Wkts\":%s,\"winner\":\"%s\"}",
                        rs.getInt("match_id"), rs.getString("format"),
                        tour.replace("\"","\\\""), ven.replace("\"","\\\""),
                        rs.getDate("match_date"),
                        rs.getString("team1").replace("\"","\\\""), rs.getString("team2").replace("\"","\\\""),
                        rs.getObject("team1_runs") != null ? rs.getInt("team1_runs") : "null",
                        rs.getObject("team1_wickets") != null ? rs.getInt("team1_wickets") : "null",
                        rs.getObject("team2_runs") != null ? rs.getInt("team2_runs") : "null",
                        rs.getObject("team2_wickets") != null ? rs.getInt("team2_wickets") : "null",
                        winner.replace("\"","\\\"")));
                }
                matchList.append("]");
                rs.close(); ps.close();

                // Build final JSON
                String json = String.format(
                    "{\"year\":%d,\"summary\":{\"totalMatches\":%d,\"totalRuns\":%d,\"totalWickets\":%d,\"totalSixes\":%d,\"totalFours\":%d}," +
                    "\"formatBreakdown\":%s,\"topRunScorers\":%s,\"topWicketTakers\":%s,\"recentMatches\":%s}",
                    year, grandMatches, grandRuns, grandWkts, grandSixes, grandFours,
                    fmtArr, topBat, topBowl, matchList);
                sendJson(ex, json);
            } catch (Exception e) {
                System.err.println("SeasonReportHandler error: " + e.getMessage());
                e.printStackTrace();
                sendJson(ex, "{\"error\":\"Error generating season report\"}");
            }
        }
    }

    // ===================== COMPARISON REPORT (generateComparisonReport) =====================
    static class ComparisonReportHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            String q = ex.getRequestURI().getQuery();
            String id1Str = getQueryParam(q, "id1");
            String id2Str = getQueryParam(q, "id2");
            if (id1Str == null || id2Str == null || id1Str.isBlank() || id2Str.isBlank()) {
                sendJson(ex, "{\"error\":\"Missing player ids (id1 and id2 required)\"}");
                return;
            }
            try (Connection conn = DatabaseConnection.getConnection()) {
                int pid1 = Integer.parseInt(id1Str);
                int pid2 = Integer.parseInt(id2Str);

                // Get both players' full reports using same logic as ReportHandler
                String p1Json = buildPlayerReportJson(conn, pid1);
                String p2Json = buildPlayerReportJson(conn, pid2);

                if (p1Json == null || p2Json == null) {
                    sendJson(ex, "{\"error\":\"One or both players not found\"}");
                    return;
                }

                // Head-to-head: matches where both played
                PreparedStatement ps = conn.prepareStatement(
                    "SELECT m.match_id, m.format, m.match_date, m.tournament, CONCAT(m.team1,' vs ',m.team2) AS matchup, " +
                    "pp1.runs_scored AS p1_runs, pp1.wickets_taken AS p1_wkts, pp1.balls_faced AS p1_balls, " +
                    "pp2.runs_scored AS p2_runs, pp2.wickets_taken AS p2_wkts, pp2.balls_faced AS p2_balls " +
                    "FROM player_performance pp1 " +
                    "JOIN player_performance pp2 ON pp1.match_id=pp2.match_id " +
                    "JOIN matches m ON pp1.match_id=m.match_id " +
                    "WHERE pp1.player_id=? AND pp2.player_id=? ORDER BY m.match_date DESC LIMIT 10");
                ps.setInt(1, pid1);
                ps.setInt(2, pid2);
                ResultSet rs = ps.executeQuery();
                StringBuilder h2h = new StringBuilder("[");
                boolean first = true;
                int p1Wins = 0, p2Wins = 0;
                while (rs.next()) {
                    if (!first) h2h.append(","); first = false;
                    int p1r = rs.getInt("p1_runs"); int p2r = rs.getInt("p2_runs");
                    int p1w = rs.getInt("p1_wkts"); int p2w = rs.getInt("p2_wkts");
                    int p1Score = p1r + p1w * 25; int p2Score = p2r + p2w * 25;
                    if (p1Score > p2Score) p1Wins++; else if (p2Score > p1Score) p2Wins++;
                    String tour = rs.getString("tournament"); if (tour == null) tour = "";
                    h2h.append(String.format("{\"matchId\":%d,\"format\":\"%s\",\"date\":\"%s\",\"tournament\":\"%s\",\"match\":\"%s\"," +
                        "\"p1Runs\":%d,\"p1Wkts\":%d,\"p1Balls\":%d,\"p2Runs\":%d,\"p2Wkts\":%d,\"p2Balls\":%d}",
                        rs.getInt("match_id"), rs.getString("format"), rs.getDate("match_date"),
                        tour.replace("\"","\\\""), rs.getString("matchup").replace("\"","\\\""),
                        p1r, p1w, rs.getInt("p1_balls"), p2r, p2w, rs.getInt("p2_balls")));
                }
                h2h.append("]");
                rs.close(); ps.close();

                String json = String.format(
                    "{\"player1\":%s,\"player2\":%s,\"headToHead\":%s,\"h2hP1Wins\":%d,\"h2hP2Wins\":%d}",
                    p1Json, p2Json, h2h, p1Wins, p2Wins);
                sendJson(ex, json);
            } catch (Exception e) {
                System.err.println("ComparisonReportHandler error: " + e.getMessage());
                e.printStackTrace();
                sendJson(ex, "{\"error\":\"Error generating comparison report\"}");
            }
        }

        private String buildPlayerReportJson(Connection conn, int pid) throws Exception {
            PreparedStatement ps = conn.prepareStatement("SELECT name, country, role, debut_year, batting_style, bowling_style FROM players WHERE player_id = ?");
            ps.setInt(1, pid);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) { rs.close(); ps.close(); return null; }
            String name = rs.getString("name");
            String country = rs.getString("country");
            String role = rs.getString("role");
            int debutYear = rs.getInt("debut_year");
            String battingStyle = rs.getString("batting_style");
            String bowlingStyle = rs.getString("bowling_style");
            rs.close(); ps.close();

            ps = conn.prepareStatement(
                "SELECT COUNT(DISTINCT pp.performance_id) AS mat, " +
                "COALESCE(SUM(pp.runs_scored),0) AS runs, COALESCE(SUM(pp.balls_faced),0) AS balls, " +
                "COALESCE(SUM(pp.wickets_taken),0) AS wkts, COALESCE(SUM(pp.overs_bowled),0) AS overs, " +
                "COALESCE(SUM(pp.runs_conceded),0) AS rc, COALESCE(SUM(pp.fours),0) AS fours, " +
                "COALESCE(SUM(pp.sixes),0) AS sixes, COALESCE(SUM(pp.catches),0) AS catches, " +
                "COALESCE(SUM(pp.maidens),0) AS maidens, " +
                "COALESCE(MAX(pp.runs_scored),0) AS highest_score, " +
                "ROUND(CASE WHEN COUNT(pp.performance_id)>0 THEN SUM(pp.runs_scored)*1.0/COUNT(pp.performance_id) ELSE 0 END,2) AS bat_avg, " +
                "ROUND(CASE WHEN SUM(pp.balls_faced)>0 THEN SUM(pp.runs_scored)*100.0/SUM(pp.balls_faced) ELSE 0 END,2) AS sr, " +
                "ROUND(CASE WHEN SUM(pp.overs_bowled)>0 THEN SUM(pp.runs_conceded)*1.0/SUM(pp.overs_bowled) ELSE 0 END,2) AS eco, " +
                "ROUND(CASE WHEN SUM(pp.wickets_taken)>0 THEN SUM(pp.runs_conceded)*1.0/SUM(pp.wickets_taken) ELSE 0 END,2) AS bowl_avg " +
                "FROM player_performance pp WHERE pp.player_id = ?");
            ps.setInt(1, pid);
            rs = ps.executeQuery();
            rs.next();
            int mat = rs.getInt("mat"); int runs = rs.getInt("runs"); int balls = rs.getInt("balls");
            int wkts = rs.getInt("wkts"); double overs = rs.getDouble("overs"); int rc = rs.getInt("rc");
            int fours = rs.getInt("fours"); int sixes = rs.getInt("sixes");
            int catches = rs.getInt("catches"); int maidens = rs.getInt("maidens");
            int hs = rs.getInt("highest_score");
            double batAvg = rs.getDouble("bat_avg"); double sr = rs.getDouble("sr");
            double eco = rs.getDouble("eco"); double bowlAvg = rs.getDouble("bowl_avg");
            rs.close(); ps.close();

            // Format breakdown
            ps = conn.prepareStatement(
                "SELECT m.format, COUNT(DISTINCT pp.performance_id) AS mat, " +
                "COALESCE(SUM(pp.runs_scored),0) AS runs, COALESCE(SUM(pp.wickets_taken),0) AS wkts, " +
                "ROUND(CASE WHEN COUNT(pp.performance_id)>0 THEN SUM(pp.runs_scored)*1.0/COUNT(pp.performance_id) ELSE 0 END,2) AS avg, " +
                "ROUND(CASE WHEN SUM(pp.balls_faced)>0 THEN SUM(pp.runs_scored)*100.0/SUM(pp.balls_faced) ELSE 0 END,2) AS sr, " +
                "ROUND(CASE WHEN SUM(pp.overs_bowled)>0 THEN SUM(pp.runs_conceded)*1.0/SUM(pp.overs_bowled) ELSE 0 END,2) AS eco " +
                "FROM player_performance pp JOIN matches m ON pp.match_id=m.match_id " +
                "WHERE pp.player_id=? GROUP BY m.format ORDER BY m.format");
            ps.setInt(1, pid);
            rs = ps.executeQuery();
            StringBuilder fmtData = new StringBuilder("[");
            boolean first = true;
            while (rs.next()) {
                if (!first) fmtData.append(","); first = false;
                fmtData.append(String.format("{\"format\":\"%s\",\"matches\":%d,\"runs\":%d,\"wickets\":%d,\"avg\":%.2f,\"sr\":%.2f,\"economy\":%.2f}",
                    rs.getString("format"), rs.getInt("mat"), rs.getInt("runs"),
                    rs.getInt("wkts"), rs.getDouble("avg"), rs.getDouble("sr"), rs.getDouble("eco")));
            }
            fmtData.append("]");
            rs.close(); ps.close();

            return String.format(
                "{\"playerId\":%d,\"name\":\"%s\",\"country\":\"%s\",\"role\":\"%s\",\"debutYear\":%d," +
                "\"battingStyle\":\"%s\",\"bowlingStyle\":\"%s\"," +
                "\"career\":{\"matches\":%d,\"runs\":%d,\"balls\":%d,\"wickets\":%d,\"overs\":%.1f," +
                "\"runsConceded\":%d,\"fours\":%d,\"sixes\":%d,\"catches\":%d,\"maidens\":%d," +
                "\"highestScore\":%d,\"battingAvg\":%.2f,\"strikeRate\":%.2f,\"economy\":%.2f,\"bowlingAvg\":%.2f}," +
                "\"formatBreakdown\":%s}",
                pid, name.replace("\"","\\\""), country.replace("\"","\\\""), role.replace("\"","\\\""), debutYear,
                nullToEmpty(battingStyle).replace("\"","\\\""), nullToEmpty(bowlingStyle).replace("\"","\\\""),
                mat, runs, balls, wkts, overs, rc, fours, sixes, catches, maidens, hs,
                batAvg, sr, eco, bowlAvg, fmtData);
        }
    }

    static class CareerStatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            String idStr = getQueryParam(ex.getRequestURI().getQuery(), "id");
            if (idStr == null || idStr.isEmpty()) {
                sendText(ex, "Missing player id");
                return;
            }
            try (Connection con = DatabaseConnection.getConnection()) {
                String sql = """
                        SELECT p.name,
                               COUNT(pp.performance_id) AS matches_played,
                               COALESCE(SUM(pp.runs_scored),0) AS total_runs,
                               COALESCE(SUM(pp.wickets_taken),0) AS total_wickets,
                               ROUND(COALESCE(AVG(CASE WHEN pp.balls_faced > 0 THEN pp.runs_scored * 100.0 / pp.balls_faced END),0),2) AS avg_strike_rate,
                               ROUND(COALESCE(AVG(CASE WHEN pp.overs_bowled > 0 THEN pp.runs_conceded / pp.overs_bowled END),0),2) AS avg_economy
                        FROM players p
                        LEFT JOIN player_performance pp ON p.player_id = pp.player_id
                        WHERE p.player_id = ?
                        GROUP BY p.player_id, p.name
                        """;
                PreparedStatement ps = con.prepareStatement(sql);
                ps.setInt(1, Integer.parseInt(idStr));
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) {
                    sendText(ex, "player not found");
                    return;
                }
                String res = "Career Stats for " + rs.getString("name") + "\n"
                        + "Matches: " + rs.getInt("matches_played") + "\n"
                        + "Total Runs: " + rs.getInt("total_runs") + "\n"
                        + "Total Wickets: " + rs.getInt("total_wickets") + "\n"
                        + "Avg Strike Rate: " + rs.getDouble("avg_strike_rate") + "\n"
                        + "Avg Economy: " + rs.getDouble("avg_economy");
                sendText(ex, res);
            } catch (Exception e) {
                System.err.println("CareerStatsHandler error: " + e.getMessage());
                sendText(ex, "FAIL");
            }
        }
    }

    static class YearlyStatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            String idStr = getQueryParam(ex.getRequestURI().getQuery(), "id");
            if (idStr == null || idStr.isEmpty()) {
                sendText(ex, "Missing player id");
                return;
            }
            try (Connection con = DatabaseConnection.getConnection()) {
                String sql = """
                        SELECT YEAR(m.match_date) AS stat_year,
                               COUNT(*) AS matches_played,
                               COALESCE(SUM(pp.runs_scored),0) AS runs,
                               COALESCE(SUM(pp.wickets_taken),0) AS wickets
                        FROM player_performance pp
                        JOIN matches m ON pp.match_id = m.match_id
                        WHERE pp.player_id = ?
                        GROUP BY YEAR(m.match_date)
                        ORDER BY stat_year
                        """;
                PreparedStatement ps = con.prepareStatement(sql);
                ps.setInt(1, Integer.parseInt(idStr));
                ResultSet rs = ps.executeQuery();

                StringBuilder sb = new StringBuilder();
                sb.append("Year-wise Stats\n");
                boolean hasRows = false;
                while (rs.next()) {
                    hasRows = true;
                    sb.append(rs.getInt("stat_year"))
                            .append(" -> Matches: ").append(rs.getInt("matches_played"))
                            .append(", Runs: ").append(rs.getInt("runs"))
                            .append(", Wickets: ").append(rs.getInt("wickets"))
                            .append("\n");
                }
                sendText(ex, hasRows ? sb.toString().trim() : "No yearly stats found");
            } catch (Exception e) {
                System.err.println("YearlyStatsHandler error: " + e.getMessage());
                sendText(ex, "FAIL");
            }
        }
    }

    static class TournamentStatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            String idStr = getQueryParam(ex.getRequestURI().getQuery(), "id");
            if (idStr == null || idStr.isEmpty()) {
                sendText(ex, "Missing player id");
                return;
            }
            try (Connection con = DatabaseConnection.getConnection()) {
                String sql = """
                        SELECT COALESCE(m.tournament, m.series, CONCAT(m.team1,' vs ',m.team2,' (',m.format,')')) AS tournament_name,
                               COUNT(*) AS matches_played,
                               COALESCE(SUM(pp.runs_scored),0) AS runs,
                               COALESCE(SUM(pp.wickets_taken),0) AS wickets
                        FROM player_performance pp
                        JOIN matches m ON pp.match_id = m.match_id
                        WHERE pp.player_id = ?
                        GROUP BY tournament_name
                        ORDER BY matches_played DESC, runs DESC
                        """;
                PreparedStatement ps = con.prepareStatement(sql);
                ps.setInt(1, Integer.parseInt(idStr));
                ResultSet rs = ps.executeQuery();

                StringBuilder sb = new StringBuilder();
                sb.append("Tournament-wise Stats\n");
                boolean hasRows = false;
                while (rs.next()) {
                    hasRows = true;
                    String tName = rs.getString("tournament_name");
                    if (tName == null || tName.isBlank()) tName = "Unknown";
                    sb.append(tName)
                            .append(" -> Matches: ").append(rs.getInt("matches_played"))
                            .append(", Runs: ").append(rs.getInt("runs"))
                            .append(", Wickets: ").append(rs.getInt("wickets"))
                            .append("\n");
                }
                sendText(ex, hasRows ? sb.toString().trim() : "No tournament stats found");
            } catch (Exception e) {
                System.err.println("TournamentStatsHandler error: " + e.getMessage());
                sendText(ex, "FAIL");
            }
        }
    }

    // add endpoints for admin operations
    static class AddMatchHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
                ex.sendResponseHeaders(405, -1); return;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(ex.getRequestBody()));
            String line = reader.readLine();
            if (line == null || line.isBlank()) {
                sendText(ex, "FAIL");
                return;
            }

            String[] parts = line.split("&");
            String date="", format="", tournament="", series="", season="", venue="", team1="", team2="";
            String tossWinner="", battingFirst="", battingSecond="", matchWinner="";
            Integer team1Runs=null, team2Runs=null, team1Wickets=null, team2Wickets=null;
            for (String p : parts) {
                if (p.startsWith("date=")) date = URLDecoder.decode(p.substring(5), StandardCharsets.UTF_8);
                if (p.startsWith("format=")) format = URLDecoder.decode(p.substring(7), StandardCharsets.UTF_8);
                if (p.startsWith("tournament=")) tournament = URLDecoder.decode(p.substring(11), StandardCharsets.UTF_8);
                if (p.startsWith("series=")) series = URLDecoder.decode(p.substring(7), StandardCharsets.UTF_8);
                if (p.startsWith("season=")) season = URLDecoder.decode(p.substring(7), StandardCharsets.UTF_8);
                if (p.startsWith("venue=")) venue = URLDecoder.decode(p.substring(6), StandardCharsets.UTF_8);
                if (p.startsWith("team1=")) team1 = URLDecoder.decode(p.substring(6), StandardCharsets.UTF_8);
                if (p.startsWith("team2=")) team2 = URLDecoder.decode(p.substring(6), StandardCharsets.UTF_8);
                if (p.startsWith("tossWinner=")) tossWinner = URLDecoder.decode(p.substring(11), StandardCharsets.UTF_8);
                if (p.startsWith("battingFirst=")) battingFirst = URLDecoder.decode(p.substring(13), StandardCharsets.UTF_8);
                if (p.startsWith("battingSecond=")) battingSecond = URLDecoder.decode(p.substring(14), StandardCharsets.UTF_8);
                if (p.startsWith("matchWinner=")) matchWinner = URLDecoder.decode(p.substring(12), StandardCharsets.UTF_8);
                if (p.startsWith("team1Runs=")) team1Runs = Integer.parseInt(p.substring(10));
                if (p.startsWith("team2Runs=")) team2Runs = Integer.parseInt(p.substring(10));
                if (p.startsWith("team1Wickets=")) team1Wickets = Integer.parseInt(p.substring(13));
                if (p.startsWith("team2Wickets=")) team2Wickets = Integer.parseInt(p.substring(13));
            }

            if (date.isBlank() || format.isBlank()) {
                sendText(ex, "FAIL");
                return;
            }

            try (Connection con = DatabaseConnection.getConnection()) {
                String sql = """
                    INSERT INTO matches(format, tournament, series, season, venue, match_date, team1, team2, team1_runs, team1_wickets, team2_runs, team2_wickets, toss_winner, batting_first, batting_second, match_winner)
                    VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                        """;
                PreparedStatement ps = con.prepareStatement(sql);
                ps.setString(1, format);
                ps.setString(2, tournament.isBlank() ? null : tournament);
                ps.setString(3, series.isBlank() ? null : series);
                ps.setString(4, season.isBlank() ? null : season);
                ps.setString(5, venue.isBlank() ? null : venue);
                ps.setDate(6, Date.valueOf(date));
                ps.setString(7, team1.isBlank() ? null : team1);
                ps.setString(8, team2.isBlank() ? null : team2);
                if (team1Runs == null) ps.setNull(9, Types.INTEGER); else ps.setInt(9, team1Runs);
                if (team1Wickets == null) ps.setNull(10, Types.INTEGER); else ps.setInt(10, team1Wickets);
                if (team2Runs == null) ps.setNull(11, Types.INTEGER); else ps.setInt(11, team2Runs);
                if (team2Wickets == null) ps.setNull(12, Types.INTEGER); else ps.setInt(12, team2Wickets);
                ps.setString(13, tossWinner.isBlank() ? null : tossWinner);
                ps.setString(14, battingFirst.isBlank() ? null : battingFirst);
                ps.setString(15, battingSecond.isBlank() ? null : battingSecond);
                ps.setString(16, matchWinner.isBlank() ? null : matchWinner);
                int updated = ps.executeUpdate();
                sendText(ex, updated > 0 ? "OK" : "FAIL");
                return;
            } catch (Exception e) {
                System.err.println("AddMatchHandler error: " + e.getMessage());
            }
            sendText(ex, "FAIL");
        }
    }

    static class DeletePlayerHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            String idStr = getQueryParam(ex.getRequestURI().getQuery(), "id");
            if (idStr == null || idStr.isBlank()) {
                sendJson(ex, "{\"success\":false,\"message\":\"Missing player id\"}");
                return;
            }
            try (Connection con = DatabaseConnection.getConnection()) {
                int pid = Integer.parseInt(idStr);
                // First delete all performances for this player
                PreparedStatement delPerf = con.prepareStatement("DELETE FROM player_performance WHERE player_id = ?");
                delPerf.setInt(1, pid);
                int perfCount = delPerf.executeUpdate();
                // Then delete the player
                PreparedStatement delPlayer = con.prepareStatement("DELETE FROM players WHERE player_id = ?");
                delPlayer.setInt(1, pid);
                int playerCount = delPlayer.executeUpdate();
                if (playerCount > 0) {
                    sendJson(ex, "{\"success\":true,\"message\":\"Player deleted successfully (" + perfCount + " performances removed)\"}");
                } else {
                    sendJson(ex, "{\"success\":false,\"message\":\"Player not found\"}");
                }
            } catch (Exception e) {
                System.err.println("DeletePlayerHandler error: " + e.getMessage());
                sendJson(ex, "{\"success\":false,\"message\":\"Error deleting player\"}");
            }
        }
    }

    static class DeleteMatchHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            String idStr = getQueryParam(ex.getRequestURI().getQuery(), "id");
            if (idStr == null || idStr.isBlank()) {
                sendJson(ex, "{\"success\":false,\"message\":\"Missing match id\"}");
                return;
            }
            try (Connection con = DatabaseConnection.getConnection()) {
                int mid = Integer.parseInt(idStr);
                // First delete all performances for this match
                PreparedStatement delPerf = con.prepareStatement("DELETE FROM player_performance WHERE match_id = ?");
                delPerf.setInt(1, mid);
                int perfCount = delPerf.executeUpdate();
                // Then delete the match
                PreparedStatement delMatch = con.prepareStatement("DELETE FROM matches WHERE match_id = ?");
                delMatch.setInt(1, mid);
                int matchCount = delMatch.executeUpdate();
                if (matchCount > 0) {
                    sendJson(ex, "{\"success\":true,\"message\":\"Match deleted successfully (" + perfCount + " performances removed)\"}");
                } else {
                    sendJson(ex, "{\"success\":false,\"message\":\"Match not found\"}");
                }
            } catch (Exception e) {
                System.err.println("DeleteMatchHandler error: " + e.getMessage());
                sendJson(ex, "{\"success\":false,\"message\":\"Error deleting match\"}");
            }
        }
    }

    // ===================== PERFORMANCE GRAPH =====================
    static class PerformanceGraphHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            String idStr = getQueryParam(ex.getRequestURI().getQuery(), "id");
            if (idStr == null || idStr.isBlank()) {
                sendJson(ex, "{\"error\":\"Missing player id\"}");
                return;
            }
            try (Connection conn = DatabaseConnection.getConnection()) {
                int pid = Integer.parseInt(idStr);
                // Get player info
                String name = "", country = "", role = "";
                PreparedStatement ps = conn.prepareStatement("SELECT name, country, role FROM players WHERE player_id = ?");
                ps.setInt(1, pid);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) { sendJson(ex, "{\"error\":\"Player not found\"}"); return; }
                name = rs.getString("name");
                country = rs.getString("country");
                role = rs.getString("role");
                rs.close(); ps.close();

                // Year-wise aggregation
                String yearSql = "SELECT YEAR(m.match_date) AS yr, COUNT(DISTINCT m.match_id) AS matches, " +
                    "COALESCE(SUM(pp.runs_scored),0) AS runs, COALESCE(SUM(pp.balls_faced),0) AS balls, " +
                    "COALESCE(SUM(pp.wickets_taken),0) AS wickets, COALESCE(SUM(pp.overs_bowled),0) AS overs, " +
                    "COALESCE(SUM(pp.runs_conceded),0) AS runs_conceded " +
                    "FROM player_performance pp JOIN matches m ON pp.match_id = m.match_id " +
                    "WHERE pp.player_id = ? GROUP BY YEAR(m.match_date) ORDER BY yr";
                ps = conn.prepareStatement(yearSql);
                ps.setInt(1, pid);
                rs = ps.executeQuery();

                StringBuilder years = new StringBuilder("[");
                StringBuilder yearData = new StringBuilder("[");
                int totalRuns = 0, totalWickets = 0, totalMatches = 0;
                boolean first = true;
                while (rs.next()) {
                    if (!first) { years.append(","); yearData.append(","); }
                    first = false;
                    int yr = rs.getInt("yr");
                    int mat = rs.getInt("matches");
                    int runs = rs.getInt("runs");
                    int balls = rs.getInt("balls");
                    int wkts = rs.getInt("wickets");
                    double overs = rs.getDouble("overs");
                    int rc = rs.getInt("runs_conceded");
                    totalRuns += runs; totalWickets += wkts; totalMatches += mat;
                    double avg = mat > 0 ? (double) runs / mat : 0;
                    double sr = balls > 0 ? (double) runs * 100.0 / balls : 0;
                    double eco = overs > 0 ? (double) rc / overs : 0;
                    years.append(yr);
                    yearData.append(String.format("{\"matches\":%d,\"runs\":%d,\"wickets\":%d,\"avg\":%.2f,\"sr\":%.2f,\"economy\":%.2f}",
                        mat, runs, wkts, avg, sr, eco));
                }
                years.append("]");
                yearData.append("]");
                rs.close(); ps.close();

                // Format-wise aggregation
                String fmtSql = "SELECT m.format, COUNT(DISTINCT m.match_id) AS matches, " +
                    "COALESCE(SUM(pp.runs_scored),0) AS runs, COALESCE(SUM(pp.balls_faced),0) AS balls, " +
                    "COALESCE(SUM(pp.wickets_taken),0) AS wickets, COALESCE(SUM(pp.overs_bowled),0) AS overs, " +
                    "COALESCE(SUM(pp.runs_conceded),0) AS runs_conceded " +
                    "FROM player_performance pp JOIN matches m ON pp.match_id = m.match_id " +
                    "WHERE pp.player_id = ? GROUP BY m.format ORDER BY m.format";
                ps = conn.prepareStatement(fmtSql);
                ps.setInt(1, pid);
                rs = ps.executeQuery();
                StringBuilder formatData = new StringBuilder("[");
                first = true;
                while (rs.next()) {
                    if (!first) formatData.append(",");
                    first = false;
                    String fmt = rs.getString("format");
                    int fmat = rs.getInt("matches");
                    int fruns = rs.getInt("runs");
                    int fballs = rs.getInt("balls");
                    int fwkts = rs.getInt("wickets");
                    double fovers = rs.getDouble("overs");
                    int frc = rs.getInt("runs_conceded");
                    double favg = fmat > 0 ? (double) fruns / fmat : 0;
                    double fsr = fballs > 0 ? (double) fruns * 100.0 / fballs : 0;
                    double feco = fovers > 0 ? (double) frc / fovers : 0;
                    formatData.append(String.format("{\"format\":\"%s\",\"matches\":%d,\"runs\":%d,\"wickets\":%d,\"avg\":%.2f,\"sr\":%.2f,\"economy\":%.2f}",
                        fmt, fmat, fruns, fwkts, favg, fsr, feco));
                }
                formatData.append("]");
                rs.close(); ps.close();

                String json = String.format("{\"name\":\"%s\",\"country\":\"%s\",\"role\":\"%s\",\"years\":%s,\"yearData\":%s,\"formatData\":%s,\"overallRuns\":%d,\"overallWickets\":%d,\"overallMatches\":%d}",
                    name.replace("\"","\\\""), country.replace("\"","\\\""), role.replace("\"","\\\""),
                    years, yearData, formatData, totalRuns, totalWickets, totalMatches);
                sendJson(ex, json);
            } catch (Exception e) {
                System.err.println("PerformanceGraphHandler error: " + e.getMessage());
                e.printStackTrace();
                sendJson(ex, "{\"error\":\"Error generating performance graph data\"}");
            }
        }
    }

    // ===================== SORT PLAYERS =====================
    static class SortPlayersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            String query = ex.getRequestURI().getQuery();
            String format = getQueryParam(query, "format");
            String criteria = getQueryParam(query, "criteria");
            if (format == null || format.isBlank()) format = "ALL";
            if (criteria == null || criteria.isBlank()) criteria = "runs";

            String orderCol;
            switch (criteria.toLowerCase()) {
                case "wickets":  orderCol = "total_wickets DESC"; break;
                case "average":  orderCol = "batting_avg DESC"; break;
                case "strikerate": orderCol = "strike_rate DESC"; break;
                case "economy": orderCol = "economy ASC"; break;
                case "matches": orderCol = "matches_played DESC"; break;
                default:        orderCol = "total_runs DESC"; break;
            }

            try (Connection conn = DatabaseConnection.getConnection()) {
                String formatFilter = format.equalsIgnoreCase("ALL") ? "" : " AND m.format = ? ";
                String sql = "SELECT p.player_id, p.name, p.country, p.role, " +
                    "COUNT(DISTINCT pp.performance_id) AS matches_played, " +
                    "COALESCE(SUM(pp.runs_scored),0) AS total_runs, " +
                    "COALESCE(SUM(pp.balls_faced),0) AS total_balls, " +
                    "COALESCE(SUM(pp.wickets_taken),0) AS total_wickets, " +
                    "COALESCE(SUM(pp.overs_bowled),0) AS total_overs, " +
                    "COALESCE(SUM(pp.runs_conceded),0) AS total_runs_conceded, " +
                    "COALESCE(SUM(pp.fours),0) AS total_fours, " +
                    "COALESCE(SUM(pp.sixes),0) AS total_sixes, " +
                    "COALESCE(SUM(pp.catches),0) AS total_catches, " +
                    "COALESCE(SUM(pp.maidens),0) AS total_maidens, " +
                    "ROUND(CASE WHEN COUNT(pp.performance_id) > 0 THEN COALESCE(SUM(pp.runs_scored),0)*1.0/COUNT(pp.performance_id) ELSE 0 END, 2) AS batting_avg, " +
                    "ROUND(CASE WHEN SUM(pp.balls_faced) > 0 THEN SUM(pp.runs_scored)*100.0/SUM(pp.balls_faced) ELSE 0 END, 2) AS strike_rate, " +
                    "ROUND(CASE WHEN SUM(pp.overs_bowled) > 0 THEN SUM(pp.runs_conceded)*1.0/SUM(pp.overs_bowled) ELSE 0 END, 2) AS economy " +
                    "FROM players p " +
                    "JOIN player_performance pp ON p.player_id = pp.player_id " +
                    "JOIN matches m ON pp.match_id = m.match_id " +
                    "WHERE 1=1 " + formatFilter +
                    "GROUP BY p.player_id, p.name, p.country, p.role " +
                    "HAVING matches_played > 0 " +
                    "ORDER BY " + orderCol;

                PreparedStatement ps = conn.prepareStatement(sql);
                if (!format.equalsIgnoreCase("ALL")) {
                    ps.setString(1, format.toUpperCase());
                }
                ResultSet rs = ps.executeQuery();

                StringBuilder sb = new StringBuilder();
                sb.append("{\"format\":\"").append(format.toUpperCase()).append("\",");
                sb.append("\"criteria\":\"").append(criteria).append("\",");
                sb.append("\"players\":[");
                boolean first = true;
                int rank = 1;
                while (rs.next()) {
                    if (!first) sb.append(",");
                    first = false;
                    sb.append("{\"rank\":").append(rank++)
                      .append(",\"playerId\":").append(rs.getInt("player_id"))
                      .append(",\"name\":\"").append(rs.getString("name").replace("\"","\\\"")).append("\"")
                      .append(",\"country\":\"").append(rs.getString("country").replace("\"","\\\"")).append("\"")
                      .append(",\"role\":\"").append(rs.getString("role").replace("\"","\\\"")).append("\"")
                      .append(",\"matches\":").append(rs.getInt("matches_played"))
                      .append(",\"runs\":").append(rs.getInt("total_runs"))
                      .append(",\"balls\":").append(rs.getInt("total_balls"))
                      .append(",\"wickets\":").append(rs.getInt("total_wickets"))
                      .append(",\"overs\":").append(rs.getDouble("total_overs"))
                      .append(",\"runsConceded\":").append(rs.getInt("total_runs_conceded"))
                      .append(",\"fours\":").append(rs.getInt("total_fours"))
                      .append(",\"sixes\":").append(rs.getInt("total_sixes"))
                      .append(",\"catches\":").append(rs.getInt("total_catches"))
                      .append(",\"maidens\":").append(rs.getInt("total_maidens"))
                      .append(",\"battingAvg\":").append(rs.getDouble("batting_avg"))
                      .append(",\"strikeRate\":").append(rs.getDouble("strike_rate"))
                      .append(",\"economy\":").append(rs.getDouble("economy"))
                      .append("}");
                }
                sb.append("]}");
                rs.close(); ps.close();
                sendJson(ex, sb.toString());
            } catch (Exception e) {
                System.err.println("SortPlayersHandler error: " + e.getMessage());
                e.printStackTrace();
                sendJson(ex, "{\"error\":\"Error sorting players\"}");
            }
        }
    }

    static class RecalculateStatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            String idStr = getQueryParam(ex.getRequestURI().getQuery(), "playerId");
            if (idStr == null || idStr.isBlank()) {
                sendText(ex, "FAIL: missing playerId");
                return;
            }

            try (Connection con = DatabaseConnection.getConnection()) {
                String sql = """
                        SELECT p.player_id,
                               p.name,
                               COUNT(pp.performance_id) AS matches_played,
                               COALESCE(SUM(pp.runs_scored),0) AS total_runs,
                               COALESCE(SUM(pp.wickets_taken),0) AS total_wickets
                        FROM players p
                        LEFT JOIN player_performance pp ON p.player_id = pp.player_id
                        WHERE p.player_id = ?
                        GROUP BY p.player_id, p.name
                        """;
                PreparedStatement ps = con.prepareStatement(sql);
                ps.setInt(1, Integer.parseInt(idStr));
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) {
                    sendText(ex, "FAIL: player not found");
                    return;
                }
                String result = "Stats refreshed for " + rs.getString("name") + "\n"
                        + "Matches: " + rs.getInt("matches_played") + "\n"
                        + "Runs: " + rs.getInt("total_runs") + "\n"
                        + "Wickets: " + rs.getInt("total_wickets") + "\n"
                        + "Note: stats are recalculated dynamically from match records.";
                sendText(ex, result);
                return;
            } catch (Exception e) {
                System.err.println("RecalculateStatsHandler error: " + e.getMessage());
            }
            sendText(ex, "FAIL");
        }
    }

    static class AddPlayerHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
                ex.sendResponseHeaders(405, -1); return;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(ex.getRequestBody()));
            String line = reader.readLine();
            if (line == null || line.isBlank()) {
                sendText(ex, "FAIL");
                return;
            }
            String[] parts = line.split("&");
            String name="", country="", role="", battingStyle="", bowlingStyle=""; int debutYear=0;
            for(String p:parts){
                if(p.startsWith("name=")) name=URLDecoder.decode(p.substring(5), StandardCharsets.UTF_8);
                if(p.startsWith("country=")) country=URLDecoder.decode(p.substring(8), StandardCharsets.UTF_8);
                if(p.startsWith("role=")) role=URLDecoder.decode(p.substring(5), StandardCharsets.UTF_8);
                if(p.startsWith("battingStyle=")) battingStyle=URLDecoder.decode(p.substring(13), StandardCharsets.UTF_8);
                if(p.startsWith("bowlingStyle=")) bowlingStyle=URLDecoder.decode(p.substring(12), StandardCharsets.UTF_8);
                if(p.startsWith("debutYear=")) { try { debutYear=Integer.parseInt(p.substring(10)); } catch(Exception ignored){} }
            }
            Player pl = new Player(0,name,country,role,battingStyle.isEmpty()?null:battingStyle,bowlingStyle.isEmpty()?null:bowlingStyle,debutYear);
            boolean ok = new PlayerRepository().save(pl);
            sendText(ex, ok?"OK":"FAIL");
        }
    }

    static class AddPerformanceHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
                ex.sendResponseHeaders(405, -1); return;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(ex.getRequestBody()));
            String line = reader.readLine();
            if (line == null || line.isBlank()) {
                sendText(ex, "FAIL");
                return;
            }
            String[] parts = line.split("&");
            int pid=0; int matchId=0; String playerTeam="";
            int runs=0, wickets=0, balls=0, conceded=0; double overs=0;
            for(String p:parts){
                if(p.startsWith("playerId=")) pid=Integer.parseInt(p.substring(9));
                if(p.startsWith("matchId=")) matchId=Integer.parseInt(p.substring(8));
                if(p.startsWith("playerTeam=")) playerTeam=URLDecoder.decode(p.substring(11), StandardCharsets.UTF_8);
                if(p.startsWith("runs=")) runs=Integer.parseInt(p.substring(5));
                if(p.startsWith("wickets=")) wickets=Integer.parseInt(p.substring(8));
                if(p.startsWith("balls=")) balls=Integer.parseInt(p.substring(6));
                if(p.startsWith("overs=")) overs=Double.parseDouble(p.substring(6));
                if(p.startsWith("conceded=")) conceded=Integer.parseInt(p.substring(9));
            }
            if (pid <= 0 || matchId <= 0) {
                sendText(ex, "FAIL");
                return;
            }
            try (Connection con = DatabaseConnection.getConnection()) {
                String matchCheckSql = "SELECT 1 FROM matches WHERE match_id = ?";
                PreparedStatement matchCheck = con.prepareStatement(matchCheckSql);
                matchCheck.setInt(1, matchId);
                ResultSet checkRs = matchCheck.executeQuery();
                if (!checkRs.next()) {
                    sendText(ex, "FAIL: match not found");
                    return;
                }

                String psql = "INSERT INTO player_performance(player_id,match_id,team_name,runs_scored,balls_faced,wickets_taken,overs_bowled,runs_conceded) VALUES(?,?,?,?,?,?,?,?)";
                PreparedStatement pps = con.prepareStatement(psql);
                pps.setInt(1,pid);
                pps.setInt(2,matchId);
                pps.setString(3, playerTeam.isBlank() ? null : playerTeam);
                pps.setInt(4,runs);
                pps.setInt(5,balls);
                pps.setInt(6,wickets);
                pps.setDouble(7,overs);
                pps.setInt(8,conceded);
                pps.executeUpdate();
                sendText(ex,"OK");
                return;
            } catch(Exception e){ System.err.println("AddPerformanceHandler error: " + e.getMessage()); }
            sendText(ex,"FAIL");
        }
    }
}
