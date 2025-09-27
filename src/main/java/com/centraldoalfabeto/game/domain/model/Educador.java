package com.centraldoalfabeto.game.domain.model;

import jakarta.persistence.*;
import java.util.Set;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "educators")
@Data
@NoArgsConstructor
public class Educador {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome")
    private String fullName;
    
    @Column(name = "email")
    private String email;
    
    @Column(name = "senha")
    private String senha;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "educator_alunos",
                     joinColumns = @JoinColumn(name = "educators_id"))
    @Column(name = "players_id")
    private Set<Long> studentIds;
}
