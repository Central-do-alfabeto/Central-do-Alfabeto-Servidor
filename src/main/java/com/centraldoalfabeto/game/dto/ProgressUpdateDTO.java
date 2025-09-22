package com.centraldoalfabeto.game.dto;

import lombok.Data;

@Data
public class ProgressUpdateDTO {
    private Integer currentPhaseIndex;
    private Integer numberOfErrors;
    private Integer numberOfSoundRepeats;
}
