package com.centraldoalfabeto.game.domain.model;

import jakarta.persistence.*;
import java.util.UUID; 
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "players")
@Data
@NoArgsConstructor
public class Jogador {
    @Id
    @Column(name = "user_id")
    private UUID userId; 

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;
    
    @Column(name = "current_phase_index")
    private Integer currentPhaseIndex;
}
