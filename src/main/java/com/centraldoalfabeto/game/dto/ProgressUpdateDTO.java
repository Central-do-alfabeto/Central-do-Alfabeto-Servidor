package com.centraldoalfabeto.game.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class ProgressUpdateDTO {
    @Min(value = 0, message = "a fase não pode ser negativa")
    private Integer currentPhaseIndex;
    
    @Min(value = 0, message = "errorsData não pode ser negativo")
    private Long errorsData; 
    
    @Min(value = 0, message = "soundRepeatsData não pode ser negativo")
    private Long soundRepeatsData;
}
