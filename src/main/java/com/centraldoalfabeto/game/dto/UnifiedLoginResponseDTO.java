package com.centraldoalfabeto.game.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@NoArgsConstructor
public class UnifiedLoginResponseDTO {
    private Long userId;
    private boolean isStudent;
    private Integer currentPhaseIndex;
    private Map<Long, String> students;

    public UnifiedLoginResponseDTO(Long userId, boolean isStudent, Integer currentPhaseIndex) {
        this.userId = userId;
        this.isStudent = isStudent;
        this.currentPhaseIndex = currentPhaseIndex;
    }

    public UnifiedLoginResponseDTO(Long userId, boolean isStudent, Map<Long, String> students) {
        this.userId = userId;
        this.isStudent = isStudent;
        this.students = students;
    }
}
