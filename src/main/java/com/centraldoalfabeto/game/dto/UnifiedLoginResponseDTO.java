package com.centraldoalfabeto.game.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
public class UnifiedLoginResponseDTO {
    private UUID userId; 

    @JsonProperty("isStudent")
    private boolean student;
    
    private Integer currentPhaseIndex; 
    
    private List<StudentSummaryDTO> studentSummaries;
    private String token;

    public UnifiedLoginResponseDTO(UUID userId, boolean isStudent, Integer currentPhaseIndex) {
        this(userId, isStudent, currentPhaseIndex, null);
    }

    public UnifiedLoginResponseDTO(UUID userId, boolean isStudent, List<StudentSummaryDTO> studentSummaries) {
        this(userId, isStudent, null, studentSummaries, null);
    }
    
    public UnifiedLoginResponseDTO(UUID userId, boolean isStudent, Integer currentPhaseIndex, String token) {
        this.userId = userId;
        this.student = isStudent;
        this.currentPhaseIndex = currentPhaseIndex;
        this.token = token;
        this.studentSummaries = null;
    }

    public UnifiedLoginResponseDTO(UUID userId, boolean isStudent, List<StudentSummaryDTO> studentSummaries, String token) {
        this.userId = userId;
        this.student = isStudent;
        this.studentSummaries = studentSummaries;
        this.token = token;
        this.currentPhaseIndex = null;
    }
    
    public UnifiedLoginResponseDTO(UUID userId, boolean isStudent, Integer currentPhaseIndex, List<StudentSummaryDTO> studentSummaries, String token) {
        this.userId = userId;
        this.student = isStudent;
        this.currentPhaseIndex = currentPhaseIndex;
        this.studentSummaries = studentSummaries;
        this.token = token;
    }
}
