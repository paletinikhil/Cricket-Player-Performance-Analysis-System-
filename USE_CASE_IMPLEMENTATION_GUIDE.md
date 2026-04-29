# Use-Case Implementation Guide

This guide maps your required use cases to current status and next implementation order.

## 1) Current status snapshot

Implemented already (working in your UI/backend):
- Login
- Add Player
- Record Performance (current endpoint creates a match stub + performance)
- Search Player
- View Players
- Compare Players (basic)
- Generate Reports (basic)
- Analyze Recent Form + Calculate Consistency (via `/api/analyze`)

Partially implemented:
- Add Match (not separate endpoint yet)
- Compare Players (currently overall only; no format-based compare in API)

Not implemented yet:
- Filter by Year
- Filter by Format
- Filter by Tournament
- View Career Stats (dedicated endpoint output)
- View Year-wise Stats
- View Tournament Stats
- Calculate Impact Score (available in service class, not exposed in API/UI)

---

## 2) Recommended implementation order (do this next)

### Phase A — Data retrieval foundation
1. Add analytics query layer using `database/use_case_query_pack.sql`.
2. Create reusable repository method(s) for:
   - combined filters,
   - career stats,
   - year-wise stats,
   - tournament-wise stats,
   - recent form,
   - consistency,
   - impact score.

### Phase B — API endpoints in `WebAppServer`
Add endpoints:
- `GET /api/addMatch` (or `POST /api/matches` preferred)
- `GET /api/filter?year=&format=&tournament=`
- `GET /api/stats/career?id=`
- `GET /api/stats/yearly?id=`
- `GET /api/stats/tournament?id=`
- `GET /api/stats/recent?id=&n=&format=`
- `GET /api/stats/consistency?id=`
- `GET /api/stats/impact?id=&format=`
- `GET /api/compare/format?id1=&id2=&format=`

### Phase C — UI wiring
In `dashboard.html` and `admin-dashboard.html`:
- Add filter panel (year, format, tournament)
- Add stats panel tabs:
  - Career
  - Year-wise
  - Tournament-wise
- Add impact score section
- Add compare-by-format section

### Phase D — Validation
Use dataset pack 2 + 3 and verify each use case with fixed test IDs:
- Batsman-heavy: `1`, `20`
- Bowler-heavy: `3`, `21`
- All-rounder: `4`, `22`

---

## 3) Important design rule for correctness

Current `AddPerformanceHandler` inserts into `matches(format,tournament,match_date)` only.
To satisfy class/use-case design fully, update it to either:
- accept existing `match_id`, **or**
- create full match record with `season`, `venue`, `team1`, `team2`, `tournament_id`.

Without this, Year/Format/Tournament reporting can become incomplete/inconsistent.

---

## 4) API response format suggestion

Use JSON everywhere for easier UI rendering:
- list endpoints -> JSON array
- stats endpoints -> JSON object with metrics
- compare endpoint -> JSON object with both players + winner field

---

## 5) SQL source of truth

Use file:
- `database/use_case_query_pack.sql`

It contains ready-to-use SQL for all required use cases in your diagram.
