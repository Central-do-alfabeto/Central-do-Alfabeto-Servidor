package com.centraldoalfabeto.game.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
public class UnifiedLoginResponseDTO {
    private Long userId;

    @JsonProperty("isStudent")
    private boolean student;
    private Integer currentPhaseIndex;
    
    private List<StudentSummaryDTO> studentSummaries;
    private String token;

    public UnifiedLoginResponseDTO(Long userId, boolean isStudent, Integer currentPhaseIndex) {
        this(userId, isStudent, currentPhaseIndex, null);
    }

    public UnifiedLoginResponseDTO(Long userId, boolean isStudent, List<StudentSummaryDTO> studentSummaries) {
        this(userId, isStudent, studentSummaries, null);
    }

    public UnifiedLoginResponseDTO(Long userId, boolean isStudent, Integer currentPhaseIndex, String token) {
        this.userId = userId;
        this.student = isStudent;
        this.currentPhaseIndex = currentPhaseIndex;
        this.token = token;
    }

    public UnifiedLoginResponseDTO(Long userId, boolean isStudent, List<StudentSummaryDTO> studentSummaries, String token) {
        this.userId = userId;
        this.student = isStudent;
        this.studentSummaries = studentSummaries;
        this.token = token;
    }
}
