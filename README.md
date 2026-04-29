# Cricket Player Performance Analysis System

This project is a Java-based cricket analytics system with a web interface for User and Admin roles.
It supports player search, filtering, analysis, comparison, reporting, and admin data management.

## 1. Technology Stack (What We Used)

- Backend language: Java
- Web server: Java built-in HttpServer (`com.sun.net.httpserver.HttpServer`)
- Database: MySQL
- Database access: JDBC + MySQL Connector/J
- Frontend: HTML + CSS + JavaScript (vanilla JS)
- UI framework: Bootstrap

## 2. Project Structure (High Level)

- `src/main` : controllers and app entry points
- `src/model` : domain/model classes
- `src/repository` : data-access classes
- `src/service` : business logic and orchestration
- `src/main/web` : frontend pages
- `database` : schema and seed SQL scripts

## 3. Build and Run

Use these commands:

```powershell
javac -cp "lib/mysql-connector-j-9.6.0.jar;src" -d out src\main\WebAppServer.java src\model\*.java src\repository\*.java src\service\*.java
java -cp ".;out;lib/mysql-connector-j-9.6.0.jar" main.WebAppServer
```

App opens at `http://localhost:8080/`.

## 4. MVC, GRASP, SOLID, and Design Patterns

This section explains what was used, where it was used, and why it was used.

### A) MVC (Model-View-Controller)

What:
- Separation of data/model, UI/view, and request handling/controller.

Where:
- Controller: `src/main/WebAppServer.java`
- Model: `src/model/*`
- View: `src/main/web/login.html`, `src/main/web/dashboard.html`, `src/main/web/admin-dashboard.html`

Why:
- Keeps code organized and easier to maintain.
- UI changes do not force business logic rewrites.

How it is used in this project:
- View (`src/main/web/*.html`) sends requests using `fetch` to backend APIs.
- Controller (`src/main/WebAppServer.java`) receives HTTP requests and routes them to the correct handler.
- Controller then calls repository/service logic, gets results, and sends JSON/text back to the view.
- Model classes (`src/model/*`) carry domain data between repository and service layers.

### B) GRASP Principles

What:
- Controller, Information Expert, High Cohesion, Low Coupling.

Where:
- Controller responsibility comment: `src/main/WebAppServer.java` (lines 25-26)
- Information Expert (data operations): `src/repository/PlayerRepository.java` (line 8 onward)
- Use-case orchestration with high cohesion: `src/service/CricketAnalysisFacade.java`

Why:

- Proper responsibility assignment.
- Better readability and reduced dependency spread.

How it is used in this project:
- Controller (GRASP): endpoint handlers in `WebAppServer` coordinate request/response flow, instead of doing all business logic inline.
- Information Expert: `PlayerRepository` handles DB persistence because it has the required data and SQL knowledge.
- High Cohesion: use-case methods are grouped in `CricketAnalysisFacade` and focused analytics stay in `PerformanceAnalyzer`.
- Low Coupling: the app layer talks to abstractions/facade rather than directly coupling every module to DB details.

### C) SOLID Principles (Implemented)

What:
- DIP, ISP, OCP.

Where:
- DIP + ISP interface: `src/repository/IPlayerRepository.java`
- DIP concrete binding: `src/repository/PlayerRepository.java` implements `IPlayerRepository`
- DIP usage in app layer: `src/main/Mainapp.java` (`IPlayerRepository repo = new PlayerRepository();`)
- OCP via strategy abstraction: `src/service/ConsistencyStrategy.java`

Why:
- Depend on abstractions, not concrete classes.
- Extend behavior with less risk to existing logic.

How it is used in this project:
- DIP: `Mainapp` depends on `IPlayerRepository` type, so repository implementation can be swapped without changing app flow.
- ISP: repository interface exposes only relevant player data operations (`findAll`, `findById`, `save`, `delete`).
- OCP: adding a new role consistency formula can be done by creating another `ConsistencyStrategy` implementation without changing the main analyzer flow.

### D) Design Patterns (Including Creational)

What:
- Facade pattern
- Strategy pattern
- Factory Method (creational, simple)

Where:
- Facade: `src/service/CricketAnalysisFacade.java`
- Strategy interface: `src/service/ConsistencyStrategy.java`
- Strategy implementations:
  - `src/service/BattingConsistencyStrategy.java`
  - `src/service/BowlingConsistencyStrategy.java`
  - `src/service/AllRounderConsistencyStrategy.java`
- Factory Method (strategy creation): `createConsistencyStrategy` in `src/service/PerformanceAnalyzer.java`

Why:
- Facade: gives a single clean API for use-cases.
- Strategy: role-based analytics can vary without changing core flow.
- Factory Method: centralizes object creation and supports cleaner extension.

How it is used in this project:
- Facade: `Mainapp` calls facade methods (`searchPlayersByName`, `analyzeConsistency`, `compareCareer`, `buildPlayerReport`) instead of coordinating many classes directly.
- Strategy: `PerformanceAnalyzer` selects role-specific calculation behavior through `ConsistencyStrategy` implementations.
- Creational (Factory Method): `createConsistencyStrategy(role)` creates the correct strategy object (batsman/bowler/all-rounder) at runtime based on role.

## 5. Key Features

- Login with role handling (User/Admin)
- Player list and search
- Filter by year, format, tournament, country, role
- Performance analysis (consistency, impact, recent form)
- Player comparison
- Reports (career, season, comparison)
- Admin operations: add player, add match, add performance, delete entities, recalculate stats

## 6. Core API Endpoints

- `POST /api/login`
- `POST /api/register`
- `GET /api/players`
- `GET /api/search`
- `GET /api/analyze`
- `GET /api/compare`
- `GET /api/report`
- `POST /api/addPlayer`
- `POST /api/addMatch`
- `POST /api/addPerformance`
- `POST /api/recalculateStats`
- `POST /api/deletePlayer`
- `POST /api/deleteMatch`

## 7. Demo Checklist (Quick Viva Flow)

1. Open `src/main/WebAppServer.java` and show MVC + GRASP controller comments.
2. Open `src/repository/IPlayerRepository.java` and `src/repository/PlayerRepository.java` for SOLID DIP/ISP.
3. Open `src/service/ConsistencyStrategy.java` and strategy implementations for OCP + Strategy pattern.
4. Open `src/service/PerformanceAnalyzer.java` and show `createConsistencyStrategy` for creational Factory Method.
5. Open `src/service/CricketAnalysisFacade.java` to show Facade orchestration.

---

This README is prepared so you can directly explain architecture decisions in your final demo.
# Cricket-Player-Performance-Analysis-System-
