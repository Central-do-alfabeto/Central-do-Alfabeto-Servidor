package com.centraldoalfabeto.game.repository;

import com.centraldoalfabeto.game.domain.model.PlayersData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlayersDataRepository extends JpaRepository<PlayersData, UUID> {
    Optional<PlayersData> findByPlayersId(UUID playerId);
}
