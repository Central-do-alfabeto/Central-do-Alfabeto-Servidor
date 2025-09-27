package com.centraldoalfabeto.game.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
public class UnifiedLoginResponseDTO {
    private Long userId;
    private boolean isStudent;
    private Integer currentPhaseIndex;
    
    private List<StudentSummaryDTO> studentSummaries;

    public UnifiedLoginResponseDTO(Long userId, boolean isStudent, Integer currentPhaseIndex) {
        this.userId = userId;
        this.isStudent = isStudent;
        this.currentPhaseIndex = currentPhaseIndex;
    }

    public UnifiedLoginResponseDTO(Long userId, boolean isStudent, List<StudentSummaryDTO> studentSummaries) {
        this.userId = userId;
        this.isStudent = isStudent;
        this.studentSummaries = studentSummaries;
    }
}
