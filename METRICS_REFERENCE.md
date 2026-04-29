# Metrics Reference

A quick guide to how the **Analyze Performance** and **Compare Players** features calculate their scores.

---

## Core Metrics

### 1. Consistency

Measures how steady a player performs across all their matches (less ups and downs = more consistent).

| Role | How it's calculated |
|------|---------------------|
| **Batsman** | Average runs minus the standard deviation of runs. A high value means the player scores well *and* doesn't fluctuate wildly between matches. |
| **Bowler** | Average wickets per match minus average economy rate. Rewards bowlers who take wickets regularly without giving away too many runs. |
| **All-Rounder** | 50% of their average runs + 50% of their average wickets per match. Balances both disciplines equally. |

> **In simple terms:** A higher consistency score means the player delivers reliable performances match after match.

---

### 2. Impact Score

Measures how much a player influences the outcome of matches, relative to their team's performance.

**Batting Impact (per match):**
- Calculates what percentage of the team's runs the player scored (run share).
- Also considers their strike rate (how fast they scored).
- Formula: **70% run share + 30% strike rate score**.

**Bowling Impact (per match):**
- Calculates what percentage of the team's wickets the player took (wicket share).
- Also considers their economy rate (lower is better).
- Formula: **70% wicket share + 30% economy score**.

**Contextual Multiplier** — the impact is boosted slightly based on match context:
- +10% if the player's team **won** the match.
- +5% if the player was batting **second** (chasing is harder).
- +3% if the player's team **won the toss**.

| Role | Final Impact |
|------|-------------|
| **Batsman** | Average batting impact across all matches |
| **Bowler** | Average bowling impact across all matches |
| **All-Rounder** | 50% batting impact + 50% bowling impact |

> **In simple terms:** A higher impact score means the player makes a bigger difference in match results.

---

### 3. Recent Form

Looks at only the **last 15 matches** (most recent first) to gauge current performance level.

| Role | How it's calculated |
|------|---------------------|
| **Batsman** | Average runs scored in the last 15 matches. |
| **Bowler** | Average wickets per match minus average economy rate (last 15 matches). |
| **All-Rounder** | 60% of average runs + 40% of average wickets (last 15 matches). |

> **In simple terms:** A higher recent form score means the player is performing well *right now*, regardless of career history.

---

## Compare Players — Verdict Scoring

When comparing two players, the system awards **1 point** per metric to whoever is better. The player with more points wins the verdict.

### Role-Specific Points

| Comparison Type | Metrics Compared (1 pt each) |
|----------------|------------------------------|
| **Batsman** | Total Runs, Strike Rate, Batting Average |
| **Bowler** | Total Wickets, Economy Rate (lower wins) |
| **All-Rounder** | Total Runs, Total Wickets |

### Common Points (all comparison types)

| Metric | 1 pt each |
|--------|-----------|
| Consistency | Higher wins |
| Recent Form | Higher wins |
| Impact Score | Higher wins |

**Total possible points:**
- Batsman comparison: **6** (3 role + 3 common)
- Bowler comparison: **5** (2 role + 3 common)
- All-Rounder comparison: **5** (2 role + 3 common)

The player with more points is declared the winner. If both have equal points, the verdict is a **Tie**.

---

*Source code: `src/service/PerformanceAnalyzer.java` (metric formulas), `src/main/WebAppServer.java` (compare verdict logic).*
