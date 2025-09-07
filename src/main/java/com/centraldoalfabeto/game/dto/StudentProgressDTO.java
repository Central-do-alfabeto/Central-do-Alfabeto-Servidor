package com.centraldoalfabeto.game.dto;

import lombok.Data;

@Data
public class StudentProgressDTO {
    private Integer currentPhaseIndex;
    private Integer[] numberOfErrorsByPhase;
    private Integer[] numberOfSoundRepeatsByPhase;
}