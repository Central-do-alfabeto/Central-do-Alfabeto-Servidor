package com.centraldoalfabeto.game.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PlayerRegistrationDTO {
    private String nome;
    private String email;
    private String senha;
}
