package com.chessfraud.gameservice.model.game;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("games")
public class Game {

    @Id
    @Column("id")
    private Integer id;
    @Column("username")
    private String username;
    @Column("moves")
    private String moves;
    @Column("legal")
    private boolean legal;

    public Game() {
    }

    public Game(String username, String moves, boolean legal) {
        this.username = username;
        this.moves = moves;
        this.legal = legal;
    }

    public Integer getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getMoves() {
        return moves;
    }

    public void setMoves(String moves) {
        this.moves = moves;
    }

    public boolean isLegal() {
        return legal;
    }

    public void setLegal(boolean legal) {
        this.legal = legal;
    }
}
