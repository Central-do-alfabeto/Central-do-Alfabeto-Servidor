package com.centraldoalfabeto.game.repository;

import com.centraldoalfabeto.game.domain.model.EducatorStudentLink;
import com.centraldoalfabeto.game.domain.model.EducatorStudentLinkId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EducatorStudentLinkRepository extends JpaRepository<EducatorStudentLink, EducatorStudentLinkId> {
    List<EducatorStudentLink> findByEducatorId(UUID educatorId);

    boolean existsByEducatorId(UUID educatorId);

    boolean existsByEducatorIdAndStudentId(UUID educatorId, UUID studentId);

    void deleteByEducatorId(UUID educatorId);
}
