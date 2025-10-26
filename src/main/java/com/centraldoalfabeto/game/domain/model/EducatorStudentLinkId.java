package com.centraldoalfabeto.game.domain.model;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class EducatorStudentLinkId implements Serializable {
    private UUID educatorId;
    private UUID studentId;

    public EducatorStudentLinkId() {
    }

    public EducatorStudentLinkId(UUID educatorId, UUID studentId) {
        this.educatorId = educatorId;
        this.studentId = studentId;
    }

    public UUID getEducatorId() {
        return educatorId;
    }

    public UUID getStudentId() {
        return studentId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EducatorStudentLinkId that = (EducatorStudentLinkId) o;
        return Objects.equals(educatorId, that.educatorId)
            && Objects.equals(studentId, that.studentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(educatorId, studentId);
    }
}
