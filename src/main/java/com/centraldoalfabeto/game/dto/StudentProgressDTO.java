package com.centraldoalfabeto.game.dto;

import lombok.Data;
import java.util.Map;

@Data
public class StudentProgressDTO {
    private Integer currentPhaseIndex;
    private Map<Integer, Integer> numberOfErrorsByPhase;
    private Map<Integer, Integer> numberOfSoundRepeatsByPhase;
}
