package com.chessfraud.userservice.user.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("users")
public class User implements Persistable<String> {

    @Id
    @Column("name")
    private String name;
    @Column("email")
    private String email;
    @Column("password")
    private String password;
    @Column("total_games")
    private int totalGames;
    @Column("cheated_games")
    private int cheatedGames;
    @Column("fair_games")
    private int fairGames;

    @Transient
    private boolean isNew = true;

    public User() {
    }

    @PersistenceCreator
    public User(String name, String email, String password, int totalGames, int cheatedGames, int fairGames) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.totalGames = totalGames;
        this.cheatedGames = cheatedGames;
        this.fairGames = fairGames;
        this.isNew = false;
    }

    @Override
    public String getId() {
        return name;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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
