package com.centraldoalfabeto.game.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "educators_students")
@IdClass(EducatorStudentLinkId.class)
public class EducatorStudentLink {
    @Id
    @Column(name = "educator_id", nullable = false)
    private UUID educatorId;

    @Id
    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    protected EducatorStudentLink() {
    }

    public EducatorStudentLink(UUID educatorId, UUID studentId) {
        this.educatorId = educatorId;
        this.studentId = studentId;
    }

    public UUID getEducatorId() {
        return educatorId;
    }

    public UUID getStudentId() {
        return studentId;
    }
}
