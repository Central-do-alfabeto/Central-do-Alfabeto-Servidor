package com.centraldoalfabeto.game.dto;

import java.util.Map;

public class UnifiedLoginResponseDTO {
    private Long userId;
    private boolean isStudent;
    private Integer currentPhaseIndex;
    private Map<Long, String> students;

    public UnifiedLoginResponseDTO(Long userId, boolean isStudent, Integer currentPhaseIndex) {
        this.userId = userId;
        this.isStudent = isStudent;
        this.currentPhaseIndex = currentPhaseIndex;
    }

    public UnifiedLoginResponseDTO(Long userId, boolean isStudent, Map<Long, String> students) {
        this.userId = userId;
        this.isStudent = isStudent;
        this.students = students;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public boolean getIsStudent() {
        return isStudent;
    }

    public void setIsStudent(boolean isStudent) {
        this.isStudent = isStudent;
    }

    public Integer getCurrentPhaseIndex() {
        return currentPhaseIndex;
    }

    public void setCurrentPhaseIndex(Integer currentPhaseIndex) {
        this.currentPhaseIndex = currentPhaseIndex;
    }

    public Map<Long, String> getStudents() {
        return students;
    }

    public void setStudents(Map<Long, String> students) {
        this.students = students;
    }
}
