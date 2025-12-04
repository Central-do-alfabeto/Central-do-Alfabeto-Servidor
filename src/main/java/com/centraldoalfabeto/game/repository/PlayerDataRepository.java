package com.centraldoalfabeto.game.repository;

import com.centraldoalfabeto.game.domain.model.PlayerData;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PlayerDataRepository extends JpaRepository<PlayerData, UUID> {
    List<PlayerData> findByPlayerIdOrderByPhaseIndexAsc(UUID playerId);

    @Query(value = """
        SELECT pd.phase_index
        FROM player_data pd
        WHERE pd.players_id = :playerId
        ORDER BY pd.phase_index DESC
        LIMIT 1
    """, nativeQuery = true)
    Optional<Integer> findLatestPhaseIndexByPlayerId(@Param("playerId") UUID playerId);

    @Query(value = """
        SELECT EXISTS (
            SELECT 1
            FROM player_data pd
            WHERE pd.players_id = :playerId
              AND pd.phase_index = :phaseIndex
        )
    """, nativeQuery = true)
    boolean existsSnapshotForPhase(@Param("playerId") UUID playerId, @Param("phaseIndex") Integer phaseIndex);

    boolean existsByPlayerId(UUID playerId);
}
