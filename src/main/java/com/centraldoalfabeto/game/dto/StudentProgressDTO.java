package com.centraldoalfabeto.game.dto;

import lombok.Data;

@Data
public class StudentProgressDTO {
    private Integer currentPhaseIndex;
    
    private Long errorsDataJson; 
    
    private Long soundRepeatsDataJson;
}
