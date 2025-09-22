package com.centraldoalfabeto.game.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UnifiedLoginResponseDTO {
    private Long userId;
    private boolean isStudent;
    private Integer currentPhaseIndex;
    private Map<Long, String> students;
}
