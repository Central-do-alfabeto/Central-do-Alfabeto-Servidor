package com.centraldoalfabeto.game.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.centraldoalfabeto.game.domain.model.PlayerData;
import com.centraldoalfabeto.game.domain.model.User;
import com.centraldoalfabeto.game.dto.ProgressUpdateDTO;
import com.centraldoalfabeto.game.repository.PlayerDataRepository;
import com.centraldoalfabeto.game.repository.UserRepository;
import com.centraldoalfabeto.game.security.JwtAuthenticatedUser;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ProgressServiceTest {

    private PlayerDataRepository playerDataRepository;
    private UserRepository userRepository;
    private ProgressService progressService;
    private UUID playerId;
    private JwtAuthenticatedUser principal;

    @BeforeEach
    void setUp() {
        playerDataRepository = mock(PlayerDataRepository.class);
        userRepository = mock(UserRepository.class);
        progressService = new ProgressService(playerDataRepository, userRepository);
        playerId = UUID.randomUUID();
        principal = new JwtAuthenticatedUser(playerId, "STUDENT", "aluno@teste.com");
        User user = new User();
        user.setId(playerId);
        when(userRepository.findById(playerId)).thenReturn(Optional.of(user));
        when(playerDataRepository.save(any(PlayerData.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private ProgressUpdateDTO dto(Integer phase, Long errors, Long repeats) {
        ProgressUpdateDTO dto = new ProgressUpdateDTO();
        dto.setCurrentPhaseIndex(phase);
        dto.setErrorsData(errors);
        dto.setSoundRepeatsData(repeats);
        return dto;
    }

    @Test
    void registraProgressoParaOAlunoAutenticado() {
        PlayerData saved = progressService.registerProgress(principal, dto(3, 2L, 5L));

        assertEquals(playerId, saved.getPlayerId());
        assertEquals(3, saved.getPhaseIndex());
        assertEquals(2L, saved.getErrosTotais());
        assertEquals(5L, saved.getReproducoesTotais());
    }

    @Test
    void valoresNegativosOuNulosSaoNormalizados() {
        ArgumentCaptor<PlayerData> captor = ArgumentCaptor.forClass(PlayerData.class);

        progressService.registerProgress(principal, dto(-4, -1L, null));

        verify(playerDataRepository).save(captor.capture());
        assertEquals(0, captor.getValue().getPhaseIndex());
        assertEquals(0L, captor.getValue().getErrosTotais());
        assertEquals(0L, captor.getValue().getReproducoesTotais());
    }

    @Test
    void faseJaRegistradaLancaConflito() {
        when(playerDataRepository.existsSnapshotForPhase(playerId, 1)).thenReturn(true);

        assertThrows(IllegalStateException.class,
            () -> progressService.registerProgress(principal, dto(1, 0L, 0L)));
        verify(playerDataRepository, never()).save(any(PlayerData.class));
    }

    @Test
    void usuarioInexistenteNaoRegistraProgresso() {
        when(userRepository.findById(playerId)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
            () -> progressService.registerProgress(principal, dto(0, 0L, 0L)));
    }

    @Test
    void proximaFaseEUltimaMaisUmOuZero() {
        when(playerDataRepository.findLatestPhaseIndexByPlayerId(playerId)).thenReturn(Optional.of(7));
        assertEquals(8, progressService.resolveNextPhaseIndex(playerId));

        UUID novo = UUID.randomUUID();
        when(playerDataRepository.findLatestPhaseIndexByPlayerId(novo)).thenReturn(Optional.empty());
        assertEquals(0, progressService.resolveNextPhaseIndex(novo));
    }
}
