package com.centraldoalfabeto.game.domain.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "players")
@Data
@NoArgsConstructor
public class Jogador {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;
    private String email;
    private String password;

    private Integer currentPhaseIndex;
    
    @Transient
    private Integer numberOfErrors;
    
    @Transient
    private Integer numberOfSoundRepeats;

    @ElementCollection
    private int[] numberOfErrorsByPhase;

    @ElementCollection
    private int[] numberOfSoundRepeatsByPhase;
    
    @PostLoad
    private void initializeArraysIfNull() {
        if (this.numberOfErrorsByPhase == null) {
            this.numberOfErrorsByPhase = new int[10];
        }
        if (this.numberOfSoundRepeatsByPhase == null) {
            this.numberOfSoundRepeatsByPhase = new int[10];
        }
    }
}
