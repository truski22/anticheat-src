package com.chessfraud.gameservice.game.repository;

import com.chessfraud.gameservice.game.model.Game;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface GameRepository extends CrudRepository<Game, Integer> {
    List<Game> findByUsername(String username);
}
