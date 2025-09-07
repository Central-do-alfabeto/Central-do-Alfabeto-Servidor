package com.centraldoalfabeto.game.dto;

import lombok.Data;

@Data
public class LoginRequestDTO {
    private String email;
    private String password;
    private Boolean isStudent;
}
