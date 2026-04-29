USE cricket_analysis;

START TRANSACTION;

-- ==============================================
-- PACK 3: EDGE-CASE DATASET FOR ROBUSTNESS TEST
-- Safe to re-run (idempotent style)
-- ==============================================

-- A) Extra players with edge profiles (IDs 19-22)
-- 19: very sparse data
-- 20: batting-heavy extreme strike rates
-- 21: bowling-heavy economy stress
-- 22: balanced but inconsistent
INSERT INTO players (player_id, name, country, role, batting_style, bowling_style, debut_year) VALUES
(19, 'KL Rahul', 'India', 'BATSMAN', 'Right-hand bat', 'None', 2014),
(20, 'Suryakumar Yadav', 'India', 'BATSMAN', 'Right-hand bat', 'Right-arm medium', 2021),
(21, 'Kuldeep Yadav', 'India', 'BOWLER', 'Left-hand bat', 'Left-arm wrist-spin', 2017),
(22, 'Hardik Pandya', 'India', 'ALL_ROUNDER', 'Right-hand bat', 'Right-arm fast-medium', 2016)
ON DUPLICATE KEY UPDATE
name = VALUES(name),
country = VALUES(country),
role = VALUES(role),
batting_style = VALUES(batting_style),
bowling_style = VALUES(bowling_style),
debut_year = VALUES(debut_year);

-- B) Extra tournament for edge tests (ID 10)
INSERT INTO tournaments (tournament_id, name, year, format_id) VALUES
(10, 'Edge Validation Cup', 2026, 3)
ON DUPLICATE KEY UPDATE
name = VALUES(name), year = VALUES(year), format_id = VALUES(format_id);

-- C) Extra matches with varied contexts (IDs 21-26)
INSERT INTO matches (match_id, format, tournament, season, venue, match_date, team1, team2, tournament_id) VALUES
(21, 'T20', 'Edge Validation Cup', '2026', 'Chennai',    '2026-01-10', 'Edge XI', 'Control XI', 10),
(22, 'T20', 'Edge Validation Cup', '2026', 'Chennai',    '2026-01-12', 'Edge XI', 'Control XI', 10),
(23, 'T20', 'Edge Validation Cup', '2026', 'Delhi',      '2026-01-15', 'Edge XI', 'Control XI', 10),
(24, 'T20', 'Edge Validation Cup', '2026', 'Delhi',      '2026-01-18', 'Edge XI', 'Control XI', 10),
(25, 'ODI', 'Edge Validation Cup', '2026', 'Mumbai',     '2026-01-22', 'Edge XI', 'Control XI', 10),
(26, 'TEST','Edge Validation Cup', '2026', 'Kolkata',    '2026-01-29', 'Edge XI', 'Control XI', 10)
ON DUPLICATE KEY UPDATE
format = VALUES(format),
tournament = VALUES(tournament),
season = VALUES(season),
venue = VALUES(venue),
match_date = VALUES(match_date),
team1 = VALUES(team1),
team2 = VALUES(team2),
tournament_id = VALUES(tournament_id);

-- D) Edge performance rows
-- cases covered:
-- 1) zero batting and zero bowling contribution
-- 2) very high strike-rate innings
-- 3) bowling-only impact
-- 4) poor economy stresscommma
-- 6) player with only one match total
INSERT INTO player_performance
(performance_id, player_id, match_id, runs_scored, balls_faced, fours, sixes, wickets_taken, overs_bowled, runs_conceded, maidens, catches)
VALUES
-- Match 21 (T20): all-zero style entries and normal controls
(2001, 19, 21, 0, 0, 0, 0, 0, 0.0, 0, 0, 0),
(2002, 20, 21, 12, 20, 1, 0, 0, 0.0, 0, 0, 0),
(2003, 21, 21, 0, 0, 0, 0, 2, 4.0, 18, 1, 1),
(2004, 22, 21, 25, 18, 3, 1, 1, 4.0, 30, 0, 0),

-- Match 22 (T20): extreme strike rate (runs > balls), plus weak spell
(2005, 19, 22, 5, 12, 0, 0, 0, 0.0, 0, 0, 0),
(2006, 20, 22, 72, 29, 5, 7, 0, 0.0, 0, 0, 1),
(2007, 21, 22, 2, 6, 0, 0, 0, 4.0, 56, 0, 0),
(2008, 22, 22, 34, 21, 2, 3, 2, 4.0, 27, 0, 0),

-- Match 23 (T20): duck + bowling hero
(2009, 19, 23, 0, 3, 0, 0, 0, 0.0, 0, 0, 0),
(2010, 20, 23, 48, 24, 3, 4, 0, 0.0, 0, 0, 0),
(2011, 21, 23, 1, 4, 0, 0, 4, 4.0, 12, 1, 0),
(2012, 22, 23, 9, 11, 1, 0, 0, 2.0, 26, 0, 1),

-- Match 24 (T20): sparse player sits out (not inserted), inconsistent all-round
(2013, 20, 24, 8, 15, 1, 0, 0, 0.0, 0, 0, 0),
(2014, 21, 24, 0, 0, 0, 0, 3, 4.0, 22, 0, 0),
(2015, 22, 24, 61, 32, 6, 3, 0, 1.0, 14, 0, 0),

-- Match 25 (ODI): long spell + low scoring anchor
(2016, 19, 25, 17, 49, 1, 0, 0, 0.0, 0, 0, 1),
(2017, 20, 25, 103, 92, 10, 4, 0, 0.0, 0, 0, 0),
(2018, 21, 25, 4, 10, 0, 0, 5, 10.0, 39, 2, 0),
(2019, 22, 25, 42, 55, 4, 0, 1, 8.0, 46, 1, 1),

-- Match 26 (TEST): heavy overs and no runs case
(2020, 19, 26, 0, 1, 0, 0, 0, 0.0, 0, 0, 0),
(2021, 20, 26, 29, 88, 2, 0, 0, 0.0, 0, 0, 0),
(2022, 21, 26, 6, 31, 0, 0, 6, 28.0, 71, 6, 0),
(2023, 22, 26, 77, 154, 8, 1, 2, 18.0, 49, 3, 1)
ON DUPLICATE KEY UPDATE
player_id = VALUES(player_id),
match_id = VALUES(match_id),
runs_scored = VALUES(runs_scored),
balls_faced = VALUES(balls_faced),
fours = VALUES(fours),
sixes = VALUES(sixes),
wickets_taken = VALUES(wickets_taken),
overs_bowled = VALUES(overs_bowled),
runs_conceded = VALUES(runs_conceded),
maidens = VALUES(maidens),
catches = VALUES(catches);

COMMIT;

-- E) Validation queries (UI + analytics sanity)
-- E1) New players only
SELECT player_id, name, role, country
FROM players
WHERE player_id BETWEEN 19 AND 22
ORDER BY player_id;

-- E2) New matches only
SELECT match_id, format, tournament, match_date, venue
FROM matches
WHERE match_id BETWEEN 21 AND 26
ORDER BY match_id;

-- E3) Aggregated edge stats
SELECT
  p.player_id,
  p.name,
  COUNT(pp.performance_id) AS matches_recorded,
  COALESCE(SUM(pp.runs_scored),0) AS total_runs,
  COALESCE(SUM(pp.wickets_taken),0) AS total_wickets,
  ROUND(COALESCE(SUM(pp.runs_scored) / NULLIF(SUM(pp.balls_faced),0) * 100, 0), 2) AS strike_rate,
  ROUND(COALESCE(SUM(pp.runs_conceded) / NULLIF(SUM(pp.overs_bowled),0), 0), 2) AS economy
FROM players p
LEFT JOIN player_performance pp ON p.player_id = pp.player_id
WHERE p.player_id BETWEEN 19 AND 22
GROUP BY p.player_id, p.name
ORDER BY p.player_id;

-- E4) Explicit edge-condition checks
SELECT performance_id, player_id, match_id, runs_scored, balls_faced, wickets_taken, overs_bowled, runs_conceded
FROM player_performance
WHERE performance_id IN (2001, 2006, 2011, 2022)
ORDER BY performance_id;
