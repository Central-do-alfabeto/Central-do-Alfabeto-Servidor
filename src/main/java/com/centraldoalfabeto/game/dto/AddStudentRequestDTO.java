package com.centraldoalfabeto.game.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AddStudentRequestDTO {
    @NotBlank(message = "o e-mail do aluno é obrigatório")
    @Email(message = "e-mail inválido")
    @Size(max = 255)
    private String email;

    @JsonAlias({"studentName", "nome", "nomeAluno"})
    @NotBlank(message = "o nome do aluno é obrigatório")
    @Size(max = 255)
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
