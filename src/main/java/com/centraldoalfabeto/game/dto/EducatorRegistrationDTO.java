package com.centraldoalfabeto.game.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class EducatorRegistrationDTO {
    @NotBlank(message = "o nome é obrigatório")
    @Size(max = 255, message = "o nome deve ter no máximo 255 caracteres")
    private String nome;
    @NotBlank(message = "o e-mail é obrigatório")
    @Email(message = "e-mail inválido")
    @Size(max = 255, message = "o e-mail deve ter no máximo 255 caracteres")
    private String email;
    @NotBlank(message = "a senha é obrigatória")
    @Size(min = 8, max = 72, message = "a senha deve ter entre 8 e 72 caracteres")
    private String senha;
}
