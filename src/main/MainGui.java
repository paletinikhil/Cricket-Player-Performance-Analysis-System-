package main;

import javafx.application.Application;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;
import model.Player;
import model.User;
import repository.AuthRepository;
import repository.PlayerRepository;

import java.io.File;
import java.util.List;

public class MainGui extends Application {
    private WebEngine engine;
    private User currentUser;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        WebView webView = new WebView();
        engine = webView.getEngine();

        loadPage("login.html");

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("app", new Bridge());
            }
        });

        Scene scene = new Scene(webView, 900, 600);
        stage.setTitle("Cricket Player Performance");
        stage.setScene(scene);
        stage.show();
    }

    private void loadPage(String filename) {
        File f = new File("src/main/web/" + filename);
        engine.load(f.toURI().toString());
    }

    public class Bridge {
        public boolean login(String user, String pass) {
            User logged = new AuthRepository().login(user, pass);
            if (logged != null) {
                currentUser = logged;
                loadPage("dashboard.html");
                engine.executeScript(String.format("setUserInfo('%s','%s')",
                        logged.getName().replace("'","\\'"),
                        logged.getRole().replace("'","\\'")
                ));
                return true;
            }
            return false;
        }

        public void logout() {
            currentUser = null;
            loadPage("login.html");
        }

        public String getAllPlayers() {
            List<Player> list = new PlayerRepository().findAll();
            StringBuilder sb = new StringBuilder();
            for (Player p : list) {
                sb.append(p.getPlayerId()).append(" - ").append(p.getName()).append("\n");
            }
            return sb.toString();
        }

        // other functionality implementations
        public String searchPlayerByName(String name) {
            List<Player> list = new PlayerRepository().findAll();
            StringBuilder sb = new StringBuilder();
            for (Player p : list) {
                if (p.getName().toLowerCase().contains(name.toLowerCase())) {
                    sb.append(p.getPlayerId()).append(" - ").append(p.getName()).append("\n");
                }
            }
            return sb.toString();
        }

        public String analyzePlayer(int id) {
            Player p = new PlayerRepository().findAll().stream()
                    .filter(x -> x.getPlayerId() == id)
                    .findFirst().orElse(null);
            if (p == null) return "player not found";
            service.PerformanceAnalyzer pa = new service.PerformanceAnalyzer();
            double consistency = pa.calculateConsistency(p);
            double recent = pa.recentForm(p, null);
            return "Consistency: " + consistency + "\nRecent form: " + recent;
        }

        public String comparePlayers(int id1, int id2) {
            List<Player> list = new PlayerRepository().findAll();
            Player p1 = list.stream().filter(x -> x.getPlayerId() == id1).findFirst().orElse(null);
            Player p2 = list.stream().filter(x -> x.getPlayerId() == id2).findFirst().orElse(null);
            if (p1 == null || p2 == null) return "one or both players not found";
            service.PlayerComparator comp = new service.PlayerComparator();
            service.ComparisonResult res = comp.compareCareer(p1, p2);
            return "Better overall: " + res.better().getName();
        }

        public String generateReport(int id) {
            Player p = new PlayerRepository().findAll().stream()
                    .filter(x -> x.getPlayerId() == id)
                    .findFirst().orElse(null);
            if (p == null) return "player not found";
            return new service.ReportGenerator().generatePlayerReportString(p);
        }

        public String addPlayer(String name, String country, String role) {
            // simple stub -- real logic would insert in database
            return "add player not yet implemented";
        }

        public String addPerformance(/* parameters */) {
            return "not implemented";
        }
    }
}
