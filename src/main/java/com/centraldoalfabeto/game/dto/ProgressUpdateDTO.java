package com.centraldoalfabeto.game.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class ProgressUpdateDTO {
    /** O jogo tem 26 fases (índices 0 a 25); o cliente envia o índice da fase concluída. */
    public static final int MAX_PHASE_INDEX = 25;

    @Min(value = 0, message = "a fase não pode ser negativa")
    @Max(value = MAX_PHASE_INDEX, message = "a fase deve estar entre 0 e 25")
    private Integer currentPhaseIndex;
    
    @Min(value = 0, message = "errorsData não pode ser negativo")
    private Long errorsData; 
    
    @Min(value = 0, message = "soundRepeatsData não pode ser negativo")
    private Long soundRepeatsData;
}
