package com.centraldoalfabeto.game.domain.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

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
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "educators_aluno", 
        joinColumns = @JoinColumn(name = "educator_id")
    )
    
    @Column(name = "student_id") 
    private Set<UUID> studentIds;
}
