package com.centraldoalfabeto.game.domain.model;

import jakarta.persistence.*;

@Entity
@Table(name = "totalaudioreproductions")
public class TotalAudioReproductions {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "players_id", referencedColumnName = "id")
    private Jogador jogador;

    @Column(name = "current_phase_index")
    private Integer currentPhaseIndex;

    @Column(name = "value")
    private Integer value;

    public TotalAudioReproductions() {
    }

    public TotalAudioReproductions(Jogador jogador, Integer currentPhaseIndex, Integer value) {
        this.jogador = jogador;
        this.currentPhaseIndex = currentPhaseIndex;
        this.value = value;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Jogador getJogador() {
        return jogador;
    }

    public void setJogador(Jogador jogador) {
        this.jogador = jogador;
    }

    public Integer getCurrentPhaseIndex() {
        return currentPhaseIndex;
    }

    public void setCurrentPhaseIndex(Integer currentPhaseIndex) {
        this.currentPhaseIndex = currentPhaseIndex;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }
}
