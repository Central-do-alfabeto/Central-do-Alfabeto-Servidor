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

    // campos auxiliares não persistidos
    @Transient
    private Integer numberOfErrors;

    @Transient
    private Integer numberOfSoundRepeats;

    // armazenados no banco como integer[]
    @Column(name = "errors_by_phase", columnDefinition = "integer[]")
    private Integer[] numberOfErrorsByPhase = new Integer[10];

    @Column(name = "repeats_by_phase", columnDefinition = "integer[]")
    private Integer[] numberOfSoundRepeatsByPhase = new Integer[10];
}