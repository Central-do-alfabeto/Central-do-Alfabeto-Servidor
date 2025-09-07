package com.centraldoalfabeto.game.domain.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Arrays;

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

    @Column(name = "errors_by_phase", columnDefinition = "integer[]")
    private Integer[] numberOfErrorsByPhase;

    @Column(name = "repeats_by_phase", columnDefinition = "integer[]")
    private Integer[] numberOfSoundRepeatsByPhase;

    @PostLoad
    @PostPersist
    public void ensureArraysInitialized() {
        if (this.numberOfErrorsByPhase == null || this.numberOfErrorsByPhase.length == 0) {
            this.numberOfErrorsByPhase = new Integer[10];
            Arrays.fill(this.numberOfErrorsByPhase, 0);
        }
        if (this.numberOfSoundRepeatsByPhase == null || this.numberOfSoundRepeatsByPhase.length == 0) {
            this.numberOfSoundRepeatsByPhase = new Integer[10];
            Arrays.fill(this.numberOfSoundRepeatsByPhase, 0);
        }
    }
}
