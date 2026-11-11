package com.centraldoalfabeto.game.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequestDTO {
    @NotBlank(message = "o e-mail é obrigatório")
    private String email;
    @NotBlank(message = "a senha é obrigatória")
    private String password;
    private Boolean isStudent;
}
