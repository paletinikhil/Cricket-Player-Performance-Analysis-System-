CREATE DATABASE IF NOT EXISTS cricket_analysis;
USE cricket_analysis;

-- USERS TABLE
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100),
    username VARCHAR(100) UNIQUE,
    password VARCHAR(100),
    role ENUM('ADMIN','USER') NOT NULL
);

-- PLAYERS TABLE
CREATE TABLE players (
    player_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    country VARCHAR(100),
    role ENUM('BATSMAN','BOWLER','ALL_ROUNDER') NOT NULL,
    batting_style VARCHAR(50),
    bowling_style VARCHAR(50),
    debut_year INT
);

-- MATCHES TABLE
CREATE TABLE matches (
    match_id INT AUTO_INCREMENT PRIMARY KEY,
    format ENUM('TEST','ODI','T20') NOT NULL,
    tournament VARCHAR(100),
    series VARCHAR(100),
    season VARCHAR(20),
    venue VARCHAR(100),
    match_date DATE,
    team1 VARCHAR(100),
    team2 VARCHAR(100),
    team1_runs INT DEFAULT NULL,
    team1_wickets INT DEFAULT NULL,
    team2_runs INT DEFAULT NULL,
    team2_wickets INT DEFAULT NULL,
    toss_winner VARCHAR(100) DEFAULT NULL,
    batting_first VARCHAR(100) DEFAULT NULL,
    batting_second VARCHAR(100) DEFAULT NULL,
    match_winner VARCHAR(100) DEFAULT NULL
);

-- PLAYER PERFORMANCE TABLE (MATCH-WISE)
CREATE TABLE player_performance (
    performance_id INT AUTO_INCREMENT PRIMARY KEY,
    player_id INT,
    match_id INT,
    team_name VARCHAR(100) DEFAULT NULL,
    runs_scored INT DEFAULT 0,
    balls_faced INT DEFAULT 0,
    fours INT DEFAULT 0,
    sixes INT DEFAULT 0,
    wickets_taken INT DEFAULT 0,
    overs_bowled DECIMAL(4,1) DEFAULT 0,
    runs_conceded INT DEFAULT 0,
    maidens INT DEFAULT 0,
    catches INT DEFAULT 0,
    FOREIGN KEY (player_id) REFERENCES players(player_id),
    FOREIGN KEY (match_id) REFERENCES matches(match_id)
);
