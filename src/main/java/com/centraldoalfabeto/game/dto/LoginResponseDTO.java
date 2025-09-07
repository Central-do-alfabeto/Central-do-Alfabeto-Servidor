package com.centraldoalfabeto.game.dto;

import java.util.Map;
import lombok.Data;

@Data
public class LoginResponseDTO {
    private Long educadorId;
    private Map<Long, String> students;
}
