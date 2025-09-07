package com.centraldoalfabeto.game.dto;

public class ProgressUpdateDTO {
    private Integer currentPhaseIndex;
    private Integer numberOfErrors;
    private Integer numberOfSoundRepeats;

    public ProgressUpdateDTO() {
    }

    public ProgressUpdateDTO(Integer currentPhaseIndex, Integer numberOfErrors, Integer numberOfSoundRepeats) {
        this.currentPhaseIndex = currentPhaseIndex;
        this.numberOfErrors = numberOfErrors;
        this.numberOfSoundRepeats = numberOfSoundRepeats;
    }

    public Integer getCurrentPhaseIndex() {
        return currentPhaseIndex;
    }

    public void setCurrentPhaseIndex(Integer currentPhaseIndex) {
        this.currentPhaseIndex = currentPhaseIndex;
    }

    public Integer getNumberOfErrors() {
        return numberOfErrors;
    }

    public void setNumberOfErrors(Integer numberOfErrors) {
        this.numberOfErrors = numberOfErrors;
    }

    public Integer getNumberOfSoundRepeats() {
        return numberOfSoundRepeats;
    }

    public void setNumberOfSoundRepeats(Integer numberOfSoundRepeats) {
        this.numberOfSoundRepeats = numberOfSoundRepeats;
    }
}
