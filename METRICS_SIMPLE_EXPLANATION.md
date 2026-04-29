# Metrics Used in This Project (Simple Explanation)

This file explains the 3 main analytics metrics used in the project:

1. Consistency
2. Impact Score
3. Recent Form

These are calculated in the service layer, mainly in:
- src/service/PerformanceAnalyzer.java

---

## 1) Consistency

### What it means
Consistency tells us how stable a player's performance is across matches.
A player who performs similarly in most matches is more consistent.

### How we calculate it
The project uses different formulas based on role:

- Batsman:
  - Consistency = Average Runs - Standard Deviation of Runs
  - If a batsman scores similar runs often, standard deviation is low, so consistency is higher.

- Bowler:
  - Consistency = Average Wickets per match - Average Economy
  - More wickets and lower economy improve consistency.

- All-rounder:
  - Consistency = 0.5 x Batting Average + 0.5 x Wickets per match
  - Equal weight to batting and bowling contribution.

### Why we used it
Because role-based consistency gives a fairer view than one single formula for everyone.

---

## 2) Impact Score

### What it means
Impact score tells how much a player's performance influenced a match result, not just raw runs/wickets.

### How we calculate it
Again, role-aware logic is used:

- Batting impact (per match):
  - Uses runs, strike rate, and contribution share in team total.
  - Then applies context multiplier (win/chase/toss signals).

- Bowling impact (per match):
  - Uses wickets, economy, and contribution share in team wickets.
  - Then applies context multiplier.

- Final impact for player:
  - Batsman: batting impact average
  - Bowler: bowling impact average
  - All-rounder: average of both sides (50%-50%)

### Why we used it
Simple totals can be misleading.
Impact score better reflects match influence and situation context.

---

## 3) Recent Form

### What it means
Recent form tells how well a player is performing in recent matches.
In this project, we use the latest 15 matches.

### How we calculate it
- First, sort matches from newest to oldest.
- Take last 15 matches.
- Then role-based calculation:

- Batsman:
  - Average runs in recent matches

- Bowler:
  - Average wickets per match - average economy

- All-rounder:
  - 60% batting recent average + 40% recent wickets average

### Why we used it
Career stats show long-term quality, but recent form shows current shape.
Both are important for analysis and comparison.

---

## Why these 3 metrics together?

- Consistency -> reliability over time
- Impact Score -> match influence quality
- Recent Form -> current performance trend

Together, they give a balanced view:
- stable player?
- match-winning player?
- currently in form?

---

## Design Note

The project uses role-based strategy logic for consistency (Strategy Pattern + Factory Method style selection), so formulas can be extended later without rewriting core flow.
