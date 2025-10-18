package com.centraldoalfabeto.game.domain.model;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "nome")
    private String nome;
    
    @Column(name = "email")
    private String email;
    
    @Column(name = "senha_hash") 
    private String senhaHash;
    
    @Column(name = "metadados", columnDefinition = "jsonb") 
    @JdbcTypeCode(SqlTypes.JSON)
    private String metadados;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Educador educador;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Jogador jogador;
}
