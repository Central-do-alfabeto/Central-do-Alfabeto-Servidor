package com.centraldoalfabeto.game.repository;

import com.centraldoalfabeto.game.domain.model.TotalErrors;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TotalErrorsRepository extends JpaRepository<TotalErrors, Long> {
    List<TotalErrors> findErrorsByPlayerId(Long playerId);
}
