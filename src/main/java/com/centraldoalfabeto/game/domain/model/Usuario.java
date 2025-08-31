package com.centraldoalfabeto.game.domain.model;

import jakarta.persistence.*;

@Entity
@Table(name = "players")
public class Jogador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;
    private String email;
    private String password;

    private Integer currentPhaseIndex;
    private Integer numberOfErrors;
    private Integer numberOfSoundRepeats;

    // Construtor vazio (obrigatório para o JPA de acordo com o Gemini)
    public Jogador() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
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

    public Integer getCurrentPhaseIndex() {
        return currentPhaseIndex;
    }

    public void setCurrentPhaseIndex(Integer currentPhaseIndex) {
        this.currentPhaseIndex = currentPhaseIndex;
    }

    public Integer getNumberOfErrors() {
        return numberOfErrors;
    }

    public void setNumberOfErrors(Integer numberOfErrors) {
        this.numberOfErrors = numberOfErrors;
    }

    public Integer getNumberOfSoundRepeats() {
        return numberOfSoundRepeats;
    }

    public void setNumberOfSoundRepeats(Integer numberOfSoundRepeats) {
        this.numberOfSoundRepeats = numberOfSoundRepeats;
    }
}
