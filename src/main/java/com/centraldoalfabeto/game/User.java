package com.centraldoalfabeto.game;

//importações
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

//declaração da Classe User
@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String password;
    private int currentPhase; 

    //Construtores
    public User() {
    }

    public User(String email, String password, int currentPhase) {
        this.email = email;
        this.password = password;
        this.currentPhase = currentPhase;
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public int getCurrentPhase() {
        return currentPhase;
    }

    public void setCurrentPhase(int currentPhase) {
        this.currentPhase = currentPhase;
    }
}