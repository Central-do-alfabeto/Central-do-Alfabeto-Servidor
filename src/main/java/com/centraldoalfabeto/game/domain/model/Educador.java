package com.centraldoalfabeto.game.domain.model;

import jakarta.persistence.*;
import java.util.UUID;
import java.util.Set;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "educators")
@Data
@NoArgsConstructor
public class Educador {
    @Id
    @Column(name = "user_id")
    private UUID userId; 

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;
    
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "educators_aluno", 
                     joinColumns = @JoinColumn(name = "educators_id"))

    @Column(name = "players_id") 
    private Set<UUID> playerIds; 
}
