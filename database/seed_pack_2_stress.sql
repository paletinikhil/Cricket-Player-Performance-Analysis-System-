USE cricket_analysis;

START TRANSACTION;

-- =============================
-- PACK 2: STRESS TEST DATASET
-- Idempotent (safe to re-run)
-- =============================

-- 1) Additional players (IDs 9-18)
INSERT INTO players (player_id, name, country, role, batting_style, bowling_style, debut_year) VALUES
(9,  'Kane Williamson',   'New Zealand', 'BATSMAN',    'Right-hand bat', 'Right-arm offbreak',        2010),
(10, 'Trent Boult',       'New Zealand', 'BOWLER',     'Right-hand bat', 'Left-arm fast-medium',      2011),
(11, 'David Warner',      'Australia',   'BATSMAN',    'Left-hand bat',  'Right-arm legbreak',        2009),
(12, 'Pat Cummins',       'Australia',   'BOWLER',     'Right-hand bat', 'Right-arm fast',            2011),
(13, 'Glenn Maxwell',     'Australia',   'ALL_ROUNDER','Right-hand bat', 'Right-arm offbreak',        2012),
(14, 'Quinton de Kock',   'South Africa','BATSMAN',    'Left-hand bat',  'None',                      2012),
(15, 'Kagiso Rabada',     'South Africa','BOWLER',     'Left-hand bat',  'Right-arm fast',            2015),
(16, 'Shakib Al Hasan',   'Bangladesh',  'ALL_ROUNDER','Left-hand bat',  'Slow left-arm orthodox',    2007),
(17, 'Jos Buttler',       'England',     'BATSMAN',    'Right-hand bat', 'None',                      2011),
(18, 'Mitchell Starc',    'Australia',   'BOWLER',     'Left-hand bat',  'Left-arm fast',             2010)
ON DUPLICATE KEY UPDATE
name = VALUES(name), country = VALUES(country), role = VALUES(role),
batting_style = VALUES(batting_style), bowling_style = VALUES(bowling_style), debut_year = VALUES(debut_year);

-- 2) Additional tournaments (IDs 6-9)
INSERT INTO tournaments (tournament_id, name, year, format_id) VALUES
(6, 'Champions Trophy', 2025, 2),
(7, 'Tri-Series', 2025, 2),
(8, 'Bilateral T20 Series', 2025, 3),
(9, 'WTC Cycle', 2025, 1)
ON DUPLICATE KEY UPDATE
name = VALUES(name), year = VALUES(year), format_id = VALUES(format_id);

-- 3) Additional matches (IDs 9-20)
INSERT INTO matches (match_id, format, tournament, season, venue, match_date, team1, team2, tournament_id) VALUES
(9,  'ODI',  'Champions Trophy',      '2025', 'Lahore',      '2025-02-20', 'Pakistan',    'Australia',   6),
(10, 'ODI',  'Champions Trophy',      '2025', 'Lahore',      '2025-02-23', 'India',       'New Zealand', 6),
(11, 'ODI',  'Tri-Series',            '2025', 'Dubai',       '2025-03-02', 'India',       'England',     7),
(12, 'ODI',  'Tri-Series',            '2025', 'Dubai',       '2025-03-05', 'Australia',   'India',       7),
(13, 'T20',  'Bilateral T20 Series',  '2025', 'Auckland',    '2025-03-20', 'New Zealand', 'Pakistan',    8),
(14, 'T20',  'Bilateral T20 Series',  '2025', 'Wellington',  '2025-03-22', 'New Zealand', 'Pakistan',    8),
(15, 'T20',  'Bilateral T20 Series',  '2025', 'Christchurch','2025-03-24', 'New Zealand', 'Pakistan',    8),
(16, 'TEST', 'WTC Cycle',             '2025', 'Lord''s',      '2025-06-10', 'England',     'Australia',   9),
(17, 'TEST', 'WTC Cycle',             '2025', 'Leeds',       '2025-06-25', 'England',     'Australia',   9),
(18, 'TEST', 'WTC Cycle',             '2025', 'Manchester',  '2025-07-10', 'England',     'Australia',   9),
(19, 'ODI',  'Champions Trophy',      '2025', 'Karachi',     '2025-02-28', 'South Africa','Bangladesh',  6),
(20, 'T20',  'Bilateral T20 Series',  '2025', 'Napier',      '2025-03-27', 'New Zealand', 'Pakistan',    8)
ON DUPLICATE KEY UPDATE
format = VALUES(format), tournament = VALUES(tournament), season = VALUES(season), venue = VALUES(venue),
match_date = VALUES(match_date), team1 = VALUES(team1), team2 = VALUES(team2), tournament_id = VALUES(tournament_id);

-- 4) Match performances (performance_id 1001+)
INSERT INTO player_performance (performance_id, player_id, match_id, runs_scored, balls_faced, fours, sixes, wickets_taken, overs_bowled, runs_conceded, maidens, catches) VALUES
(1001, 5,  9,  74, 86, 6, 1, 0, 0.0,  0, 0, 1),
(1002, 6,  9,   9, 14, 1, 0, 3, 9.0, 41, 1, 0),
(1003, 11, 9,  67, 72, 7, 1, 0, 0.0,  0, 0, 0),
(1004, 12, 9,  18, 21, 2, 0, 2,10.0, 48, 1, 1),
(1005, 13, 9,  43, 31, 4, 2, 1, 6.0, 34, 0, 0),
(1006, 18, 9,   6,  9, 0, 0, 2,10.0, 50, 0, 0),

(1007, 1, 10,  89, 95, 8, 2, 0, 0.0,  0, 0, 0),
(1008, 2, 10,  52, 48, 5, 1, 0, 0.0,  0, 0, 1),
(1009, 3, 10,   5,  9, 0, 0, 3,10.0, 37, 2, 0),
(1010, 9, 10,  61, 70, 5, 1, 0, 0.0,  0, 0, 0),
(1011, 10,10,  11, 15, 1, 0, 2,10.0, 44, 1, 0),
(1012, 16,10,  37, 42, 3, 0, 1, 8.0, 46, 0, 1),

(1013, 1, 11,  44, 49, 4, 0, 0, 0.0,  0, 0, 0),
(1014, 7, 11,  72, 88, 7, 1, 2, 8.0, 39, 1, 1),
(1015, 8, 11,  81, 97, 9, 0, 0, 0.0,  0, 0, 0),
(1016, 17,11,  63, 58, 6, 2, 0, 0.0,  0, 0, 1),
(1017, 12,11,  14, 19, 1, 0, 3,10.0, 45, 1, 0),
(1018, 18,11,   7, 11, 1, 0, 1,10.0, 54, 0, 0),

(1019, 11,12, 101,102,10, 2, 0, 0.0,  0, 0, 0),
(1020, 12,12,  22, 25, 2, 0, 2,10.0, 43, 1, 0),
(1021, 13,12,  48, 33, 5, 2, 1, 5.0, 29, 0, 1),
(1022, 1, 12,  57, 64, 6, 1, 0, 0.0,  0, 0, 1),
(1023, 3, 12,   3,  6, 0, 0, 2,10.0, 52, 0, 0),
(1024, 4, 12,  35, 40, 3, 1, 1, 8.0, 36, 1, 0),

(1025, 9, 13,  38, 29, 4, 1, 0, 0.0,  0, 0, 0),
(1026, 10,13,   5,  8, 0, 0, 2, 4.0, 23, 0, 1),
(1027, 5, 13,  56, 41, 6, 2, 0, 0.0,  0, 0, 0),
(1028, 6, 13,   2,  4, 0, 0, 1, 4.0, 28, 0, 0),
(1029, 16,13,  27, 22, 2, 1, 1, 4.0, 31, 0, 0),
(1030, 15,13,   8, 11, 1, 0, 2, 4.0, 26, 0, 0),

(1031, 9, 14,  73, 52, 8, 2, 0, 0.0,  0, 0, 1),
(1032, 10,14,  14, 13, 2, 0, 3, 4.0, 19, 1, 0),
(1033, 5, 14,  45, 36, 4, 1, 0, 0.0,  0, 0, 0),
(1034, 6, 14,   1,  3, 0, 0, 2, 4.0, 24, 0, 0),
(1035, 17,14,  39, 24, 4, 2, 0, 0.0,  0, 0, 0),
(1036, 3, 14,   0,  0, 0, 0, 1, 4.0, 30, 0, 1),

(1037, 9, 15,  28, 24, 3, 1, 0, 0.0,  0, 0, 0),
(1038, 10,15,   9,  7, 1, 0, 2, 4.0, 18, 0, 0),
(1039, 5, 15,  61, 43, 7, 2, 0, 0.0,  0, 0, 1),
(1040, 6, 15,   4,  5, 0, 0, 2, 4.0, 21, 0, 0),
(1041, 11,15,  33, 27, 3, 1, 0, 0.0,  0, 0, 0),
(1042, 18,15,   7,  8, 1, 0, 1, 4.0, 33, 0, 0),

(1043, 7, 16,  96,180,11, 1, 2,15.0, 44, 2, 1),
(1044, 8, 16, 121,210,12, 1, 0, 0.0,  0, 0, 0),
(1045, 11,16,  58,109, 6, 0, 0, 0.0,  0, 0, 0),
(1046, 12,16,  31, 70, 3, 0, 4,23.0, 67, 5, 0),
(1047, 18,16,  14, 32, 2, 0, 3,24.0, 72, 4, 0),
(1048, 4, 16,  63,140, 7, 0, 2,21.0, 59, 3, 1),

(1049, 7, 17,  71,132, 8, 1, 1,12.0, 33, 1, 0),
(1050, 8, 17,  84,165, 9, 0, 0, 0.0,  0, 0, 0),
(1051, 11,17, 104,201,10, 1, 0, 0.0,  0, 0, 0),
(1052, 12,17,  16, 41, 2, 0, 5,25.0, 61, 6, 1),
(1053, 18,17,   9, 28, 1, 0, 4,22.0, 58, 4, 0),
(1054, 3, 17,  11, 34, 1, 0, 3,21.0, 49, 3, 0),

(1055, 7, 18,  42, 95, 5, 0, 2,14.0, 46, 1, 1),
(1056, 8, 18,  63,140, 7, 0, 0, 0.0,  0, 0, 0),
(1057, 11,18,  88,166, 9, 1, 0, 0.0,  0, 0, 1),
(1058, 12,18,  24, 52, 3, 0, 3,20.0, 55, 2, 0),
(1059, 18,18,  12, 30, 1, 0, 4,23.0, 69, 4, 0),
(1060, 4, 18,  38,101, 4, 0, 2,18.0, 47, 2, 0),

(1061, 14,19,  91,101, 9, 1, 0, 0.0,  0, 0, 1),
(1062, 15,19,  13, 18, 1, 0, 3,10.0, 42, 1, 0),
(1063, 16,19,  69, 84, 6, 1, 2, 8.0, 37, 0, 1),
(1064, 5, 19,  41, 52, 4, 0, 0, 0.0,  0, 0, 0),
(1065, 6, 19,   3,  7, 0, 0, 2, 9.0, 46, 0, 0),
(1066, 10,19,   6,  9, 0, 0, 1,10.0, 53, 0, 0),

(1067, 9, 20,  55, 39, 6, 2, 0, 0.0,  0, 0, 0),
(1068, 10,20,   3,  5, 0, 0, 2, 4.0, 20, 1, 0),
(1069, 5, 20,  62, 45, 7, 2, 0, 0.0,  0, 0, 1),
(1070, 6, 20,   1,  3, 0, 0, 2, 4.0, 27, 0, 0),
(1071, 17,20,  34, 20, 3, 2, 0, 0.0,  0, 0, 0),
(1072, 3, 20,   0,  0, 0, 0, 1, 4.0, 29, 0, 1)
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

-- 5) Quick sanity checks
SELECT COUNT(*) AS total_players FROM players;
SELECT COUNT(*) AS total_matches FROM matches;
SELECT COUNT(*) AS total_performance_rows FROM player_performance;

SELECT p.player_id, p.name,
       COUNT(pp.performance_id) AS matches_played,
       COALESCE(SUM(pp.runs_scored),0) AS total_runs,
       COALESCE(SUM(pp.wickets_taken),0) AS total_wickets
FROM players p
LEFT JOIN player_performance pp ON p.player_id = pp.player_id
GROUP BY p.player_id, p.name
ORDER BY matches_played DESC, total_runs DESC;
