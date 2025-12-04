package com.centraldoalfabeto.game.service;

import com.centraldoalfabeto.game.domain.model.PlayerData;
import com.centraldoalfabeto.game.domain.model.User;
import com.centraldoalfabeto.game.dto.ProgressUpdateDTO;
import com.centraldoalfabeto.game.repository.PlayerDataRepository;
import com.centraldoalfabeto.game.security.JwtAuthenticatedUser;
import com.centraldoalfabeto.game.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProgressService {
    private final PlayerDataRepository playerDataRepository;
    private final UserRepository userRepository;

    @Autowired
    public ProgressService(PlayerDataRepository playerDataRepository, UserRepository userRepository) {
        this.playerDataRepository = playerDataRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public PlayerData registerProgress(JwtAuthenticatedUser authenticatedUser, ProgressUpdateDTO dto) {
        UUID playerId = authenticatedUser.getUserId();
        int phaseIndex = dto.getCurrentPhaseIndex() != null ? dto.getCurrentPhaseIndex() : 0;
        if (phaseIndex < 0) {
            phaseIndex = 0;
        }

        User player = userRepository.findById(playerId)
            .orElseThrow(() -> new IllegalStateException("Usuário não encontrado para registrar progresso."));

        if (playerDataRepository.existsSnapshotForPhase(playerId, phaseIndex)) {
            throw new IllegalStateException("Progresso da fase já registrado para este jogador.");
        }

        long totalErrors = dto.getErrorsData() != null ? Math.max(dto.getErrorsData(), 0L) : 0L;
        long totalRepeats = dto.getSoundRepeatsData() != null ? Math.max(dto.getSoundRepeatsData(), 0L) : 0L;

        PlayerData snapshot = new PlayerData();
        snapshot.setPlayerId(playerId);
        snapshot.setPhaseIndex(phaseIndex);
        snapshot.setErrosTotais(totalErrors);
        snapshot.setReproducoesTotais(totalRepeats);
        snapshot.setPlayer(player);

        return playerDataRepository.save(snapshot);
    }

    public int resolveNextPhaseIndex(UUID playerId) {
        return playerDataRepository.findLatestPhaseIndexByPlayerId(playerId)
            .map(latest -> latest + 1)
            .orElse(0);
    }
}
