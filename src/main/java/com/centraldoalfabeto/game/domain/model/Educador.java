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

    private String fullName;
    private String email;
    private String password;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "educator_students", joinColumns = @JoinColumn(name = "educator_id"))
    @Column(name = "student_id")
    private Set<Long> studentIds;
}
