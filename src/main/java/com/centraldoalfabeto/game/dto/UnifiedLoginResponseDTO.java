package com.centraldoalfabeto.game.dto;

import lombok.Data;
import java.util.Map;

@Data
public class UnifiedLoginResponseDTO {
    private Long userId;
    private boolean isStudent;
    private Integer currentPhaseIndex;
    private Map<Long, String> students;
}
