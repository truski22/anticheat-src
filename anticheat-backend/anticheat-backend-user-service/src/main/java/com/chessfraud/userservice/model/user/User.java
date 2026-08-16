package com.chessfraud.userservice.model.user;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;


@Table("USERS")
public class User {
    @Id
    private long id;
    private String name;
    private String password;
    private String email;
    private int totalGames;
    private int cheatedGames;
    private int fairGames;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getTotalGames() {
        return totalGames;
    }

    public void setTotalGames(int totalGames) {
        this.totalGames = totalGames;
    }

    public int getCheatedGames() {
        return cheatedGames;
    }

    public void setCheatedGames(int cheatedGames) {
        this.cheatedGames = cheatedGames;
    }

    public int getFairGames() {
        return fairGames;
    }

    public void setFairGames(int fairGames) {
        this.fairGames = fairGames;
    }
}
