import mysql.connector
import random
from datetime import datetime, timedelta

# Connect to MySQL
conn = mysql.connector.connect(
    host="localhost",
    user="root",
    password="sathwik",
    database="cricket_analysis"
)

cursor = conn.cursor()

# Insert Users
cursor.execute("""
INSERT INTO users (name, username, password, role)
VALUES
('Admin User', 'admin', 'admin123', 'ADMIN'),
('Normal User', 'user1', 'user123', 'USER')
""")

# Sample Players
players = [
    ("Virat Kohli", "India", "BATSMAN", "Right-hand Bat", None, 2008),
    ("Babar Azam", "Pakistan", "BATSMAN", "Right-hand Bat", None, 2015),
    ("Joe Root", "England", "BATSMAN", "Right-hand Bat", None, 2012),
    ("Jasprit Bumrah", "India", "BOWLER", None, "Right-arm Fast", 2016),
    ("Shaheen Afridi", "Pakistan", "BOWLER", None, "Left-arm Fast", 2018),
    ("Rashid Khan", "Afghanistan", "BOWLER", None, "Leg Spin", 2015),
    ("Ben Stokes", "England", "ALL_ROUNDER", "Left-hand Bat", "Right-arm Medium", 2013),
    ("Hardik Pandya", "India", "ALL_ROUNDER", "Right-hand Bat", "Right-arm Medium", 2016)
]

for p in players:
    cursor.execute("""
    INSERT INTO players (name, country, role, batting_style, bowling_style, debut_year)
    VALUES (%s,%s,%s,%s,%s,%s)
    """, p)

# Create Matches
formats = ["TEST","ODI","T20"]
tournaments = ["World Cup","Asia Cup","Bilateral Series","Champions Trophy"]

for i in range(60):
    date = datetime.now() - timedelta(days=random.randint(1, 1500))
    cursor.execute("""
    INSERT INTO matches (format, tournament, season, venue, match_date, team1, team2)
    VALUES (%s,%s,%s,%s,%s,%s,%s)
    """, (
        random.choice(formats),
        random.choice(tournaments),
        "2024",
        "Stadium " + str(random.randint(1,10)),
        date.date(),
        "TeamA",
        "TeamB"
    ))

# Generate Player Performance
cursor.execute("SELECT player_id, role FROM players")
players_db = cursor.fetchall()

cursor.execute("SELECT match_id, format FROM matches")
matches_db = cursor.fetchall()

for player in players_db:
    for match in random.sample(matches_db, 30):  # 30 matches per player
        runs = random.randint(0,150)
        wickets = random.randint(0,5)

        cursor.execute("""
        INSERT INTO player_performance
        (player_id, match_id, runs_scored, balls_faced, fours, sixes,
         wickets_taken, overs_bowled, runs_conceded, maidens, catches)
        VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)
        """, (
            player[0],
            match[0],
            runs if player[1] != "BOWLER" else random.randint(0,30),
            random.randint(1,100),
            random.randint(0,15),
            random.randint(0,10),
            wickets if player[1] != "BATSMAN" else random.randint(0,1),
            round(random.uniform(0,10),1),
            random.randint(0,80),
            random.randint(0,3),
            random.randint(0,5)
        ))

conn.commit()
cursor.close()
conn.close()

print("Database Fully Populated Successfully!")
