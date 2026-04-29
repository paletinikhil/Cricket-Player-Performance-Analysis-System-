package model;

public class Admin extends User {

    public Admin(int id, String name, String username, String password) {
        super(id, name, username, password, "ADMIN");
    }

    // admin-specific operations could be implemented here or delegated to services
    public void addPlayer(Player player) {
        // delegate to repository from service layer
    }

    public void updatePlayer(Player player) {}
    public void deletePlayer(int id) {}
    public void addMatch(Match m) {}
    public void manageSeason(Season s) {}
}
