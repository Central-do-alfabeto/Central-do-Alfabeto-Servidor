package com.centraldoalfabeto.game.domain.controller;

import com.centraldoalfabeto.game.dto.PlayerRegistrationDTO;
import com.centraldoalfabeto.game.dto.ProgressUpdateDTO;
import com.centraldoalfabeto.game.dto.UnifiedLoginResponseDTO;
import com.centraldoalfabeto.game.security.JwtAuthenticatedUser;
import com.centraldoalfabeto.game.service.JogadorService;
import com.centraldoalfabeto.game.service.ProgressService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import com.centraldoalfabeto.game.config.ApiErrors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/players")
public class JogadorController {
    private static final Logger logger = LoggerFactory.getLogger(JogadorController.class);

    private final JogadorService jogadorService;
    private final ProgressService progressService;

    @Autowired
    public JogadorController(
        JogadorService jogadorService, 
        ProgressService progressService
    ) {
        this.jogadorService = jogadorService;
        this.progressService = progressService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerPlayer(@Valid @RequestBody PlayerRegistrationDTO registrationDTO) {
        if (registrationDTO.getEmail() == null || registrationDTO.getSenha() == null) {
             return ResponseEntity.badRequest().build();
        }

        try {
            UnifiedLoginResponseDTO responseDTO = jogadorService.registerPlayer(registrationDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
            
        } catch (IllegalArgumentException | DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiErrors.of("E-mail já cadastrado."));
        } catch (Exception e) {
            logger.error("Erro no registro do jogador: ", e);
            return ResponseEntity.internalServerError().body(ApiErrors.of("Erro interno."));
        }
    }

    @PostMapping("/{id}/progress")
    public ResponseEntity<?> updateProgress(
            @PathVariable UUID id, 
            @Valid @RequestBody ProgressUpdateDTO progressData,
            @AuthenticationPrincipal JwtAuthenticatedUser authenticatedUser) {

        if (authenticatedUser == null
                || !"STUDENT".equalsIgnoreCase(authenticatedUser.getRole())
                || !authenticatedUser.getUserId().equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        logger.info("Dados recebidos para atualização para player {}: {}", id, progressData);

        try {
            progressService.registerProgress(authenticatedUser, progressData);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (IllegalStateException | DataIntegrityViolationException e) {
            logger.warn("Registro duplicado de progresso para player {} na fase {}", id, progressData.getCurrentPhaseIndex());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiErrors.of("Progresso desta fase já registrado."));
        } catch (Exception e) {
            logger.error("Erro na atualização do progresso: ", e);
            return ResponseEntity.internalServerError().body(ApiErrors.of("Erro interno."));
        }
    }
}
