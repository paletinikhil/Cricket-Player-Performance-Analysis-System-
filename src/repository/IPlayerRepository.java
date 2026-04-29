package repository;

import java.util.List;
import model.Player;

// SOLID (ISP + DIP): upper layers depend on this abstraction instead of concrete PlayerRepository.
public interface IPlayerRepository {
    List<Player> findAll();
    Player findById(int playerId);
    boolean save(Player player);
    boolean delete(int playerId);
}
