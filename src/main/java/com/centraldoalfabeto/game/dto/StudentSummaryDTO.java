package com.centraldoalfabeto.game.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID; 

@Data
@NoArgsConstructor
public class StudentSummaryDTO {
    private UUID id; 
    private String fullName;
    private Integer currentPhaseIndex;
    
    private String errorsDataJson; 
    private String soundRepeatsDataJson;
    
    public StudentSummaryDTO(UUID id, String fullName, Integer currentPhaseIndex) {
        this.id = id;
        this.fullName = fullName;
        this.currentPhaseIndex = currentPhaseIndex;
    }
}
