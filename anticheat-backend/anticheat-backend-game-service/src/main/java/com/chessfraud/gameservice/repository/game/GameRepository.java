package com.chessfraud.gameservice.repository.game;

import com.chessfraud.gameservice.model.game.Game;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface GameRepository extends CrudRepository<Game, Integer> {
    List<Game> findByUsername(String username);
}
