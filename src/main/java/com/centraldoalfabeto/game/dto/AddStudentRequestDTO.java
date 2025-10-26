package com.centraldoalfabeto.game.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public class AddStudentRequestDTO {
    private String email;

    @JsonAlias({"studentName", "nome", "nomeAluno"})
    private String studentName;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }
}
