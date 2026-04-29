USE cricket_analysis;

/*
=========================================================
Use-Case SQL Query Pack (MySQL 8)
Covers:
- Login, Add Player, Add Match, Record Performance
- Search + Filters (Year / Format / Tournament)
- Career / Year-wise / Tournament-wise stats
- Recent Form / Consistency / Impact Score
- Compare Players / Report data
=========================================================
*/

-- ------------------------------------------------------
-- 0) ONE-TIME MIGRATION FOR TEAM-BASED IMPACT
-- ------------------------------------------------------
-- Run once on existing DBs (skip if columns already exist):
-- ALTER TABLE matches
--   ADD COLUMN team1_runs INT NULL,
--   ADD COLUMN team1_wickets INT NULL,
--   ADD COLUMN team2_runs INT NULL,
--   ADD COLUMN team2_wickets INT NULL,
--   ADD COLUMN toss_winner VARCHAR(100) NULL,
--   ADD COLUMN batting_first VARCHAR(100) NULL,
--   ADD COLUMN batting_second VARCHAR(100) NULL,
--   ADD COLUMN match_winner VARCHAR(100) NULL;
--
-- ALTER TABLE player_performance
--   ADD COLUMN team_name VARCHAR(100) NULL;

-- ------------------------------------------------------
-- 1) Recommended indexes for API speed
-- ------------------------------------------------------
-- Run these once if indexes are missing:
-- CREATE INDEX idx_players_name ON players(name);
-- CREATE INDEX idx_matches_date ON matches(match_date);
-- CREATE INDEX idx_matches_format ON matches(format);
-- CREATE INDEX idx_matches_tournament ON matches(tournament);
-- CREATE INDEX idx_perf_player ON player_performance(player_id);
-- CREATE INDEX idx_perf_match ON player_performance(match_id);

-- ------------------------------------------------------
-- 2) LOGIN
-- ------------------------------------------------------
-- params: username, password
SELECT id, name, username, role
FROM users
WHERE username = ? AND password = ?;

-- ------------------------------------------------------
-- 3) ADD PLAYER
-- ------------------------------------------------------
INSERT INTO players(name, country, role, batting_style, bowling_style, debut_year)
VALUES (?, ?, ?, ?, ?, ?);

-- ------------------------------------------------------
-- 4) ADD MATCH (separate use case)
-- ------------------------------------------------------
INSERT INTO matches(format, tournament, season, venue, match_date, team1, team2, team1_runs, team1_wickets, team2_runs, team2_wickets, toss_winner, batting_first, batting_second, match_winner)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);

-- ------------------------------------------------------
-- 5) RECORD PERFORMANCE
-- ------------------------------------------------------
INSERT INTO player_performance(
  player_id, match_id, team_name, runs_scored, balls_faced, fours, sixes,
  wickets_taken, overs_bowled, runs_conceded, maidens, catches
)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);

-- ------------------------------------------------------
-- 6) BASE VIEW FOR ANALYTICS
-- ------------------------------------------------------
CREATE OR REPLACE VIEW v_player_match_perf AS
SELECT
  p.player_id,
  p.name AS player_name,
  p.country,
  p.role AS player_role,
  m.match_id,
  m.format,
  m.tournament,
  m.season,
  m.venue,
  m.match_date,
  m.team1,
  m.team2,
  m.team1_runs,
  m.team1_wickets,
  m.team2_runs,
  m.team2_wickets,
  m.toss_winner,
  m.batting_first,
  m.batting_second,
  m.match_winner,
  YEAR(m.match_date) AS match_year,
  pp.performance_id,
  pp.team_name,
  pp.runs_scored,
  pp.balls_faced,
  pp.fours,
  pp.sixes,
  pp.wickets_taken,
  pp.overs_bowled,
  pp.runs_conceded,
  pp.maidens,
  pp.catches,
  CASE
    WHEN pp.team_name IS NOT NULL AND m.team1 IS NOT NULL AND LOWER(pp.team_name) = LOWER(m.team1) THEN m.team1_runs
    WHEN pp.team_name IS NOT NULL AND m.team2 IS NOT NULL AND LOWER(pp.team_name) = LOWER(m.team2) THEN m.team2_runs
    ELSE NULL
  END AS team_total_runs,
  CASE
    WHEN pp.team_name IS NOT NULL AND m.team1 IS NOT NULL AND LOWER(pp.team_name) = LOWER(m.team1) THEN m.team1_wickets
    WHEN pp.team_name IS NOT NULL AND m.team2 IS NOT NULL AND LOWER(pp.team_name) = LOWER(m.team2) THEN m.team2_wickets
    ELSE NULL
  END AS team_total_wickets,
  CASE WHEN pp.balls_faced > 0 THEN ROUND(pp.runs_scored * 100.0 / pp.balls_faced, 2) ELSE 0 END AS strike_rate,
  CASE WHEN pp.overs_bowled > 0 THEN ROUND(pp.runs_conceded / pp.overs_bowled, 2) ELSE 0 END AS economy
FROM player_performance pp
JOIN players p ON p.player_id = pp.player_id
JOIN matches m ON m.match_id = pp.match_id;

-- ------------------------------------------------------
-- 6) SEARCH PLAYER
-- ------------------------------------------------------
-- params: keyword (e.g., "vir")
SELECT player_id, name, country, role, batting_style, bowling_style, debut_year
FROM players
WHERE name LIKE CONCAT('%', ?, '%')
ORDER BY name;

-- ------------------------------------------------------
-- 7) FILTER BY YEAR / FORMAT / TOURNAMENT
-- ------------------------------------------------------
-- 7a: by year
-- params: year
SELECT *
FROM v_player_match_perf
WHERE match_year = ?
ORDER BY match_date DESC;

-- 7b: by format (TEST / ODI / T20)
-- params: format
SELECT *
FROM v_player_match_perf
WHERE format = ?
ORDER BY match_date DESC;

-- 7c: by tournament
-- params: tournament name exact (or change to LIKE)
SELECT *
FROM v_player_match_perf
WHERE tournament = ?
ORDER BY match_date DESC;

-- 7d: combined filter (dynamic API version)
-- params: yearNullable, formatNullable, tournamentNullable
SELECT *
FROM v_player_match_perf
WHERE (? IS NULL OR match_year = ?)
  AND (? IS NULL OR format = ?)
  AND (? IS NULL OR tournament = ?)
ORDER BY match_date DESC;

-- ------------------------------------------------------
-- 8) VIEW CAREER STATS (per player)
-- ------------------------------------------------------
-- params: player_id
SELECT
  player_id,
  player_name,
  COUNT(*) AS matches_played,
  SUM(runs_scored) AS total_runs,
  ROUND(AVG(runs_scored),2) AS avg_runs,
  ROUND(AVG(strike_rate),2) AS avg_strike_rate,
  SUM(wickets_taken) AS total_wickets,
  ROUND(AVG(economy),2) AS avg_economy,
  SUM(catches) AS total_catches
FROM v_player_match_perf
WHERE player_id = ?
GROUP BY player_id, player_name;

-- ------------------------------------------------------
-- 9) VIEW YEAR-WISE STATS
-- ------------------------------------------------------
-- params: player_id
SELECT
  match_year,
  COUNT(*) AS matches_played,
  SUM(runs_scored) AS runs,
  SUM(wickets_taken) AS wickets,
  ROUND(AVG(strike_rate),2) AS avg_strike_rate,
  ROUND(AVG(economy),2) AS avg_economy
FROM v_player_match_perf
WHERE player_id = ?
GROUP BY match_year
ORDER BY match_year;

-- ------------------------------------------------------
-- 10) VIEW TOURNAMENT-WISE STATS
-- ------------------------------------------------------
-- params: player_id
SELECT
  tournament,
  COUNT(*) AS matches_played,
  SUM(runs_scored) AS runs,
  SUM(wickets_taken) AS wickets,
  ROUND(AVG(strike_rate),2) AS avg_strike_rate,
  ROUND(AVG(economy),2) AS avg_economy
FROM v_player_match_perf
WHERE player_id = ?
GROUP BY tournament
ORDER BY matches_played DESC, runs DESC;

-- ------------------------------------------------------
-- 12) ANALYZE RECENT FORM (last N matches, role-based)
-- ------------------------------------------------------
-- params: player_id, N (e.g., 5/10/15)
SELECT
  t.player_id,
  t.player_name,
  t.player_role,
  COUNT(*) AS matches_considered,
  ROUND(
    CASE
      WHEN UPPER(t.player_role) = 'BATSMAN' THEN AVG(t.runs_scored)
      WHEN UPPER(t.player_role) = 'BOWLER' THEN AVG(t.wickets_taken) - AVG(t.economy)
      WHEN UPPER(t.player_role) = 'ALL_ROUNDER' THEN (AVG(t.runs_scored) * 0.6) + (AVG(t.wickets_taken) * 0.4)
      ELSE 0
    END
  , 4) AS recent_form_score
FROM (
  SELECT *
  FROM v_player_match_perf
  WHERE player_id = ?
  ORDER BY match_date DESC
  LIMIT ?
) t
GROUP BY t.player_id, t.player_name, t.player_role;

-- ------------------------------------------------------
-- 13) CALCULATE CONSISTENCY (role-based)
-- ------------------------------------------------------
-- params: player_id
SELECT
  player_id,
  player_name,
  player_role,
  ROUND(
    CASE
      WHEN UPPER(player_role) = 'BATSMAN' THEN AVG(runs_scored) - STDDEV_POP(runs_scored)
      WHEN UPPER(player_role) = 'BOWLER' THEN AVG(wickets_taken) - AVG(economy)
      WHEN UPPER(player_role) = 'ALL_ROUNDER' THEN (AVG(runs_scored) * 0.5) + (AVG(wickets_taken) * 0.5)
      ELSE 0
    END,
    4
  ) AS consistency_score
FROM v_player_match_perf
WHERE player_id = ?
GROUP BY player_id, player_name, player_role;

-- ------------------------------------------------------
-- 14) CALCULATE IMPACT SCORE (format-wise, team contribution)
-- ------------------------------------------------------
-- params: player_id, format
-- Notes:
-- - Batting impact includes run contribution to team + strike rate
-- - Bowling impact includes wicket contribution to team + economy
-- - All-rounder impact uses 50:50
SELECT
  player_id,
  player_name,
  player_role,
  format,
  ROUND(
    CASE
      WHEN UPPER(player_role) = 'BATSMAN' THEN
        AVG(
          (0.7 * (CASE WHEN team_total_runs IS NOT NULL AND team_total_runs > 0 THEN (runs_scored * 100.0 / team_total_runs) ELSE 0 END))
          +
          (0.3 * LEAST(100, strike_rate / 2.0))
        )
      WHEN UPPER(player_role) = 'BOWLER' THEN
        AVG(
          (0.7 * (CASE WHEN team_total_wickets IS NOT NULL AND team_total_wickets > 0 THEN (wickets_taken * 100.0 / team_total_wickets) ELSE 0 END))
          +
          (0.3 * GREATEST(0, LEAST(100, (10 - economy) * 10)))
        )
      WHEN UPPER(player_role) = 'ALL_ROUNDER' THEN
        (
          AVG(
            (0.7 * (CASE WHEN team_total_runs IS NOT NULL AND team_total_runs > 0 THEN (runs_scored * 100.0 / team_total_runs) ELSE 0 END))
            +
            (0.3 * LEAST(100, strike_rate / 2.0))
          )
          +
          AVG(
            (0.7 * (CASE WHEN team_total_wickets IS NOT NULL AND team_total_wickets > 0 THEN (wickets_taken * 100.0 / team_total_wickets) ELSE 0 END))
            +
            (0.3 * GREATEST(0, LEAST(100, (10 - economy) * 10)))
          )
        ) / 2
      ELSE 0
    END,
    4
  ) AS impact_score
FROM v_player_match_perf
WHERE player_id = ?
  AND format = ?
GROUP BY player_id, player_name, player_role, format;

-- ------------------------------------------------------
-- 14) COMPARE PLAYERS (overall)
-- ------------------------------------------------------
-- params: player_id_1, player_id_2
WITH s AS (
  SELECT
    player_id,
    player_name,
    COUNT(*) AS matches_played,
    SUM(runs_scored) AS runs,
    SUM(wickets_taken) AS wickets,
    ROUND(AVG(strike_rate),2) AS avg_sr,
    ROUND(AVG(economy),2) AS avg_econ
  FROM v_player_match_perf
  WHERE player_id IN (?, ?)
  GROUP BY player_id, player_name
)
SELECT * FROM s ORDER BY runs DESC, wickets DESC;

-- ------------------------------------------------------
-- 15) GENERATE REPORT DATASET (single-player report packet)
-- ------------------------------------------------------
-- params: player_id

-- 15a profile + career headline
SELECT
  p.player_id,
  p.name,
  p.country,
  p.role,
  p.batting_style,
  p.bowling_style,
  p.debut_year,
  COUNT(pp.performance_id) AS matches_played,
  COALESCE(SUM(pp.runs_scored),0) AS total_runs,
  COALESCE(SUM(pp.wickets_taken),0) AS total_wickets
FROM players p
LEFT JOIN player_performance pp ON pp.player_id = p.player_id
WHERE p.player_id = ?
GROUP BY p.player_id, p.name, p.country, p.role, p.batting_style, p.bowling_style, p.debut_year;

-- 15b year-wise
SELECT
  YEAR(m.match_date) AS year,
  COUNT(*) AS matches_played,
  SUM(pp.runs_scored) AS runs,
  SUM(pp.wickets_taken) AS wickets
FROM player_performance pp
JOIN matches m ON m.match_id = pp.match_id
WHERE pp.player_id = ?
GROUP BY YEAR(m.match_date)
ORDER BY year;

-- 15c tournament-wise
SELECT
  m.tournament,
  COUNT(*) AS matches_played,
  SUM(pp.runs_scored) AS runs,
  SUM(pp.wickets_taken) AS wickets
FROM player_performance pp
JOIN matches m ON m.match_id = pp.match_id
WHERE pp.player_id = ?
GROUP BY m.tournament
ORDER BY matches_played DESC, runs DESC;

-- 15d latest 10 innings/spells
SELECT
  m.match_date,
  m.format,
  m.tournament,
  pp.runs_scored,
  pp.balls_faced,
  pp.wickets_taken,
  pp.overs_bowled,
  pp.runs_conceded
FROM player_performance pp
JOIN matches m ON m.match_id = pp.match_id
WHERE pp.player_id = ?
ORDER BY m.match_date DESC
LIMIT 10;
