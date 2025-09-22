package com.centraldoalfabeto.game.repository;

import com.centraldoalfabeto.game.domain.model.TotalAudioReproductions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TotalAudioReproductionsRepository extends JpaRepository<TotalAudioReproductions, Long> {
    List<TotalAudioReproductions> findSoundRepeatsByPlayerId(Long playerId);
}
