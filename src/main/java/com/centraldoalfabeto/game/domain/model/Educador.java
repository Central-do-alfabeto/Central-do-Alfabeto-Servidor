package com.centraldoalfabeto.game.domain.model;

import jakarta.persistence.*;
import java.util.Set;

@Entity
@Table(name = "educators")
public class Educador {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;
    private String email;
    private String password;

    @ElementCollection
    @CollectionTable(name = "educator_students", joinColumns = @JoinColumn(name = "educator_id"))
    @Column(name = "student_id")
    private Set<Long> studentIds;

    // Construtor vazio (obrigatório para o JPA de acordo com o Gemini)
    public Educador() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Set<Long> getStudentIds() {
        return studentIds;
    }

    public void setStudentIds(Set<Long> studentIds) {
        this.studentIds = studentIds;
    }
}
