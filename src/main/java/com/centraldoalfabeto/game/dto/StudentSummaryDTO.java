package com.centraldoalfabeto.game.dto;

import lombok.Data;
import java.util.Map;

@Data
public class StudentSummaryDTO {
    private Long id;
    private String fullName;
    private Integer currentPhaseIndex;
    private Map<Integer, Integer> numberOfErrorsByPhase;
    private Map<Integer, Integer> numberOfSoundRepeatsByPhase;

    public StudentSummaryDTO(Long id, String fullName, Integer currentPhaseIndex) {
        this.id = id;
        this.fullName = fullName;
        this.currentPhaseIndex = currentPhaseIndex;
    }
}
