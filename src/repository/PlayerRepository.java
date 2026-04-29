package repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.*;

// GRASP (Information Expert): persistence logic stays in repository.
// SOLID (DIP): class fulfills IPlayerRepository abstraction.
public class PlayerRepository implements IPlayerRepository {

    public List<Player> findAll() {

        List<Player> players = new ArrayList<>();

        try (Connection con = DatabaseConnection.getConnection()) {

            String sql = "SELECT * FROM players";
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                Player player = new Player(
                        rs.getInt("player_id"),
                        rs.getString("name"),
                        rs.getString("country"),
                        rs.getString("role"),
                        rs.getString("batting_style"),
                        rs.getString("bowling_style"),
                        rs.getInt("debut_year")
                );

                player.setPerformances(getPerformances(player.getPlayerId(), con));
                players.add(player);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return players;
    }

    public Player findById(int playerId) {
        List<Player> all = findAll();
        return all.stream().filter(p -> p.getPlayerId() == playerId).findFirst().orElse(null);
    }

    public boolean save(Player player) {
        try (Connection con = DatabaseConnection.getConnection()) {
            String sql = "INSERT INTO players(name,country,role,batting_style,bowling_style,debut_year) VALUES(?,?,?,?,?,?)";
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, player.getName());
            ps.setString(2, player.getCountry());
            ps.setString(3, player.getRole());
            ps.setString(4, player.getBattingStyle());
            ps.setString(5, player.getBowlingStyle());
            if (player.getDebutYear() > 0) ps.setInt(6, player.getDebutYear()); else ps.setNull(6, java.sql.Types.INTEGER);
            int updated = ps.executeUpdate();
            if (updated > 0) {
                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next()) player = new Player(keys.getInt(1), player.getName(), player.getCountry(), player.getRole(), player.getBattingStyle(), player.getBowlingStyle(), player.getDebutYear());
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean delete(int playerId) {
        try (Connection con = DatabaseConnection.getConnection()) {
            String sql = "DELETE FROM players WHERE player_id = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, playerId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // other filtering can be done at service level using the returned list

    private List<PlayerPerformance> getPerformances(int playerId, Connection con) throws Exception {

        List<PlayerPerformance> performances = new ArrayList<>();

        String sql = """
            SELECT p.*, m.match_id, m.match_date, m.format, m.tournament,
                   m.team1, m.team2, m.team1_runs, m.team1_wickets, m.team2_runs, m.team2_wickets,
                   m.toss_winner, m.batting_first, m.batting_second, m.match_winner
                FROM player_performance p
                JOIN matches m ON p.match_id = m.match_id
                WHERE p.player_id = ?
                """;

        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, playerId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {

            Match match = new Match(
                    rs.getInt("match_id"),
                    rs.getDate("match_date").toLocalDate(),
                    rs.getString("format"),
                    rs.getString("tournament"),
                    rs.getString("team1"),
                    rs.getString("team2"),
                    (Integer) rs.getObject("team1_runs"),
                    (Integer) rs.getObject("team1_wickets"),
                    (Integer) rs.getObject("team2_runs"),
                        (Integer) rs.getObject("team2_wickets"),
                        rs.getString("toss_winner"),
                        rs.getString("batting_first"),
                        rs.getString("batting_second"),
                        rs.getString("match_winner")
            );

            PlayerPerformance performance = new PlayerPerformance(
                    rs.getInt("runs_scored"),
                    rs.getInt("wickets_taken"),
                    rs.getInt("balls_faced"),
                    rs.getDouble("overs_bowled"),
                    rs.getInt("runs_conceded"),
                    rs.getString("team_name"),
                    match
            );

            performances.add(performance);
        }

        return performances;
    }
}
