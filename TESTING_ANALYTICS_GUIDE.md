# Testing Guide: Consistency, Recent Form, and Impact Score

## Overview
This guide walks you through testing all three analytics features end-to-end:
1. **Consistency** - Measures player stability (lower std dev = higher consistency)
2. **Recent Form** - Average of last 15 matches performance
3. **Impact Score** - Match-contextual impact with multipliers

---

## Admin Workflow: Adding Test Data

### Step 1: Add Two Players

**Player 1: Consistent Batsman**
- Name: `Virat Kohli`
- Country: `India`
- Role: `BATSMAN`

**Player 2: Inconsistent Bowler**
- Name: `Jasprit Bumrah`
- Country: `India`
- Role: `BOWLER`

**Action:** In admin dashboard, click **"Add Player"** and enter above data twice.

---

### Step 2: Add 5 Matches with Context Fields

Create matches where you control the toss/batting/match winner for impact testing. Each match requires:
- Teams (Team 1, Team 2): Any two teams (e.g., India vs Australia)
- Date: Any date (e.g., 2024-01-01)
- Format: Choose ODI, TEST, or T20
- **NEW FIELDS:**
  - **Toss Winner:** Who won the toss (India or Australia)
  - **Batting First:** Team batting first
  - **Batting Second:** Team batting second  
  - **Match Winner:** Which team won the match

**Suggested Matches:**

| Match ID | Team1 | Team2 | Date | Format | Toss | Batting1 | Batting2 | Winner | Purpose |
|----------|-------|-------|------|--------|------|----------|----------|--------|---------|
| 1 | India | Australia | 2024-01-01 | ODI | India | India | Australia | India | Impact: Toss +3%, Batting first, Won +10% |
| 2 | India | Australia | 2024-01-05 | ODI | Australia | Australia | India | India | Impact: Batting 2nd +5%, Won +10% |
| 3 | India | Australia | 2024-01-10 | ODI | India | India | Australia | Australia | Impact: Toss +3%, Batting first, Lost 0% |
| 4 | India | England | 2024-01-15 | T20 | India | India | England | India | Impact: Toss +3%, Batting first, Won +10% |
| 5 | India | England | 2024-01-20 | T20 | England | England | India | India | Impact: Batting 2nd +5%, Won +10% |

**Action:** In admin dashboard, click **"Add Match"** 5 times. For each match, fill:
- Team1, Team2, Date, Format (required)
- Team1 Runs, Team1 Wickets, Team2 Runs, Team2 Wickets (enter any values, e.g., 280/5 and 270/8)
- **Toss Winner, Batting First, Batting Second, Match Winner** (new context fields)

---

### Step 3: Record Performances for Both Players

For **Virat Kohli** (Batsman) - Record 5 performances in the 5 matches:

| Match ID | Role | Runs | Balls | Wickets | Overs | Runs Conceded | Notes |
|----------|------|------|-------|---------|-------|---------------|-------|
| 1 | Batsman | 85 | 95 | 0 | 0 | 0 | Strong start |
| 2 | Batsman | 90 | 92 | 0 | 0 | 0 | Consistent |
| 3 | Batsman | 75 | 88 | 0 | 0 | 0 | Good innings |
| 4 | Batsman | 45 | 35 | 0 | 0 | 0 | Aggressive T20 |
| 5 | Batsman | 88 | 55 | 0 | 0 | 0 | Best T20 score |

For **Jasprit Bumrah** (Bowler) - Record 5 performances in the 5 matches:

| Match ID | Role | Runs | Balls | Wickets | Overs | Runs Conceded | Notes |
|----------|------|------|-------|---------|-------|---------------|-------|
| 1 | Bowler | 0 | 0 | 2 | 10 | 45 | Good economy |
| 2 | Bowler | 0 | 0 | 3 | 7 | 35 | Excellent bowling |
| 3 | Bowler | 0 | 0 | 1 | 9 | 52 | Weaker spell |
| 4 | Bowler | 0 | 0 | 1 | 4 | 28 | T20 spell |
| 5 | Bowler | 0 | 0 | 2 | 3.5 | 26 | Impactful T20 |

**Action:** In admin dashboard, click **"Record Performance"** 10 times (5 for each player), selecting the Match ID and entering performance metrics.

---

### Step 4: Verify Data in Admin Dashboard

Click **"View Matches"** to confirm all 5 matches are stored with correct toss/batting/winner fields.

Click **"Maintain Records"** to confirm both players are in the system with their performance data.

---

## Testing Analytics: Verify All 3 Metrics

### Access Test Analytics Panel

1. Open admin dashboard
2. Click **"Test Analytics"** button (bottom of action grid)
3. Select a player from dropdown
4. Click **"Analyze Player"**

This will display:
- **Consistency:** Standard deviation-based consistency score
- **Recent form:** Average of last 15 matches
- **Impact score:** Impact multiplied by match context (toss, batting order, outcome)

---

## Expected Results

### For Virat Kohli (Batsman):

**Consistency Calculation:**
- Runs: 85, 90, 75, 45, 88
- Mean runs: 76.6
- Std dev: ~16.4
- **Consistency = Mean - StdDev ≈ 60.2** (higher = more consistent)

**Recent Form:**
- Average of last 15 (only 5 matches exist): (85+90+75+45+88) / 5 = **76.6 runs/match**

**Impact Score (Average Across Matches):**
- Match 1: Base impact × 1.13 (1.0 + 0.10 won + 0.03 toss winner)
- Match 2: Base impact × 1.15 (1.0 + 0.10 won + 0.05 batting 2nd)
- Match 3: Base impact × 1.03 (1.0 + 0.03 toss winner only, lost match)
- Match 4: Base impact × 1.13 (1.0 + 0.10 won + 0.03 toss winner)
- Match 5: Base impact × 1.15 (1.0 + 0.10 won + 0.05 batting 2nd)
- **Average impact: Higher values show context multiplier is working**

### For Jasprit Bumrah (Bowler):

**Consistency Calculation:**
- Wickets: 2, 3, 1, 1, 2 (avg 1.8)
- Economy: 4.5, 5.0, 5.78, 7.0, 7.43
- **Consistency = Avg Wickets - Avg Economy ≈ -5.4** (negative because economy higher than average wickets)

**Recent Form:**
- Wickets average: (2+3+1+1+2) / 5 = **1.8 wickets/match**
- Economy average: (4.5+5.0+5.78+7.0+7.43) / 5 ≈ **5.94 runs/over**
- **Recent form metric = Wickets - Economy ≈ -4.14**

**Impact Score:**
- Similar to batsman, context multipliers apply based on match outcome/toss/batting order

---

## Verification Checklist

✅ **Consistency Metric:**
- [ ] Virat Kohli shows high consistency (lower std dev of runs)
- [ ] Jasprit Bumrah shows consistency metric based on wickets vs economy

✅ **Recent Form Metric:**
- [ ] Virat Kohli recent form ≈ 76-77 runs/match
- [ ] Jasprit Bumrah recent form shows wickets - economy calculation

✅ **Impact Score Metric:**
- [ ] Impact score visible in Test Analytics output
- [ ] Impact values reflect base impact × context multiplier
- [ ] Multiplier increases when player's team won match (+10%)
- [ ] Multiplier increases when player batted 2nd (+5%)
- [ ] Multiplier increases when player's team won toss (+3%)

---

## How Multipliers Work (Impact Score Context)

Impact score = Base impact × Context multiplier

**Context Multiplier = 1.0 + bonus**

Bonuses applied:
- +10% if player's team won the match
- +5% if player batted second in the match
- +3% if player's team won the toss

Example: If Virat Kohli's team won the match (player batted first, won toss):
- Multiplier = 1.0 + 0.10 = **1.10** (10% boost)

Example: If Virat Kohli's team won the match (player batted second, lost toss):
- Multiplier = 1.0 + 0.10 + 0.05 = **1.15** (15% boost)

---

## Troubleshooting

**Issue:** Test Analytics button not visible
- **Solution:** Refresh admin dashboard. Recompile if still missing.

**Issue:** Impact score shows as 0
- **Solution:** Check if calculateImpact() is returning 0 for format. Try `/api/analyze?id=X` endpoint directly with `null` format parameter (calculates across all formats).

**Issue:** Consistency shows unexpected value
- **Solution:** Verify performance data was recorded correctly in "Maintain Records" view. Check runs/wickets values are not null.

---

## Next Steps

After verifying all 3 metrics work:
1. Update user dashboard to show impact score alongside recent form/consistency
2. Consider separate "Impact Analysis" section or tab in user analytics
3. Test across different match formats (ODI, T20, TEST) if needed

---
