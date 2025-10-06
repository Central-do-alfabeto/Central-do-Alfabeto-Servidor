package com.centraldoalfabeto.game.domain.model;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "players_data")
@Data
@NoArgsConstructor
public class PlayersData {
    @Id
    @Column(name = "players_id")
    private UUID playersId; 

    @OneToOne
    @MapsId
    @JoinColumn(name = "players_id")
    private Jogador player;
    
    @Column(name = "erros_totais", columnDefinition = "jsonb") 
    private String errosTotais;
    
    @Column(name = "audios_totais", columnDefinition = "jsonb") 
    private String audiosTotais;

    @Column(name = "phase_index")
    private Integer phaseIndex;
}
