package com.centraldoalfabeto.game.service;

import com.centraldoalfabeto.game.domain.model.PlayersData;
import com.centraldoalfabeto.game.dto.ProgressUpdateDTO;
import com.centraldoalfabeto.game.repository.PlayersDataRepository;
import com.centraldoalfabeto.game.security.JwtAuthenticatedUser;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.Optional;

@Service
public class ProgressService {
    private final PlayersDataRepository playersDataRepository;

    @Autowired
    public ProgressService(PlayersDataRepository playersDataRepository) {
        this.playersDataRepository = playersDataRepository;
    }

    @Transactional
    public PlayersData updateProgress(JwtAuthenticatedUser authenticatedUser, ProgressUpdateDTO dto) {
        UUID playerId = authenticatedUser.getUserId();
        
        Optional<PlayersData> existingData = playersDataRepository.findById(playerId);
        
        PlayersData playerData = existingData.orElseGet(() -> {
            PlayersData newPlayersData = new PlayersData();
            newPlayersData.setPlayersId(playerId);
            newPlayersData.setErrosTotais(0L);
            newPlayersData.setAudiosTotais(0L);
            newPlayersData.setPhaseIndex(0);
            return newPlayersData;
        });
        
        if (dto.getCurrentPhaseIndex() != null) {
             playerData.setPhaseIndex(dto.getCurrentPhaseIndex());
        }
        
        if (dto.getErrorsData() != null) {
            playerData.setErrosTotais(dto.getErrorsData());
        }
        
        if (dto.getSoundRepeatsData() != null) {
            playerData.setAudiosTotais(dto.getSoundRepeatsData());
        }

        return playersDataRepository.save(playerData);
    }
}
