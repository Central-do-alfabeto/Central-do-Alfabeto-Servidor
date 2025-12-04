package com.centraldoalfabeto.game.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "player_data")
@Data
@NoArgsConstructor
public class PlayerData {
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "players_id", nullable = false)
    private UUID playerId;

    @Column(name = "phase_index", nullable = false)
    private Integer phaseIndex;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "players_id", referencedColumnName = "id", insertable = false, updatable = false)
    private User player;

    @Column(name = "erros_totais")
    private Long errosTotais = 0L;

    @Column(name = "reproducoes_totais")
    private Long reproducoesTotais = 0L;
}
