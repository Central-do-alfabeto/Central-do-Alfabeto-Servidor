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

    @ElementCollection
    private int[] numberOfErrorsByPhase;

    @ElementCollection
    private int[] numberOfSoundRepeatsByPhase;

    public Jogador() {
        this.numberOfErrorsByPhase = new int[50];
        this.numberOfSoundRepeatsByPhase = new int[50];
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

    public int[] getNumberOfErrorsByPhase() {
        return numberOfErrorsByPhase;
    }

    public void setNumberOfErrorsByPhase(int[] numberOfErrorsByPhase) {
        this.numberOfErrorsByPhase = numberOfErrorsByPhase;
    }

    public int[] getNumberOfSoundRepeatsByPhase() {
        return numberOfSoundRepeatsByPhase;
    }

    public void setNumberOfSoundRepeatsByPhase(int[] numberOfSoundRepeatsByPhase) {
        this.numberOfSoundRepeatsByPhase = numberOfSoundRepeatsByPhase;
    }
}
