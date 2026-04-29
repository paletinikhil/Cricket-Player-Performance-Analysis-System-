package service;

import model.Player;
import model.Format;

import java.util.List;
import java.util.stream.Collectors;

public class PlayerSearchService {

    public List<Player> searchByName(List<Player> players, String name) {
        return players.stream()
                .filter(p -> p.getName().toLowerCase().contains(name.toLowerCase()))
                .collect(Collectors.toList());
    }

    public List<Player> searchByCountry(List<Player> players, String country) {
        return players.stream()
                .filter(p -> p.getCountry().equalsIgnoreCase(country))
                .collect(Collectors.toList());
    }

    public List<Player> searchByRole(List<Player> players, String role) {
        return players.stream()
                .filter(p -> p.getRole().equalsIgnoreCase(role))
                .collect(Collectors.toList());
    }

    public List<Player> filterByYear(List<Player> players, int year) {
        return players.stream()
                .filter(p -> p.getStatsByYear().containsKey(year))
                .collect(Collectors.toList());
    }

    public List<Player> filterByTournament(List<Player> players, String tournament) {
        return players.stream()
                .filter(p -> p.getStatsByTournament().containsKey(tournament))
                .collect(Collectors.toList());
    }

    public List<Player> filterByFormat(List<Player> players, Format format) {
        return players.stream()
                .filter(p -> p.getStatsByFormat().containsKey(format))
                .collect(Collectors.toList());
    }

    public List<Player> sortPlayers(List<Player> players, java.util.Comparator<Player> comparator) {
        return players.stream().sorted(comparator).collect(Collectors.toList());
    }
}
