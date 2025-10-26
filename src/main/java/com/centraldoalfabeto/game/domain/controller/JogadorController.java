package com.centraldoalfabeto.game.domain.controller;

import com.centraldoalfabeto.game.dto.PlayerRegistrationDTO;
import com.centraldoalfabeto.game.dto.ProgressUpdateDTO;
import com.centraldoalfabeto.game.dto.UnifiedLoginResponseDTO;
import com.centraldoalfabeto.game.security.JwtAuthenticatedUser;
import com.centraldoalfabeto.game.service.JogadorService;
import com.centraldoalfabeto.game.service.ProgressService;
import org.springframework.beans.factory.annotation.Autowired;
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
    public ResponseEntity<UnifiedLoginResponseDTO> registerPlayer(@RequestBody PlayerRegistrationDTO registrationDTO) {
        if (registrationDTO.getEmail() == null || registrationDTO.getSenha() == null) {
             return ResponseEntity.badRequest().build();
        }

        try {
            UnifiedLoginResponseDTO responseDTO = jogadorService.registerPlayer(registrationDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            logger.error("Erro no registro do jogador: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{id}/updateProgress")
    public ResponseEntity<Void> updateProgress(
            @PathVariable UUID id, 
            @RequestBody ProgressUpdateDTO progressData,
            @AuthenticationPrincipal JwtAuthenticatedUser authenticatedUser) {

        if (authenticatedUser == null
                || !"STUDENT".equalsIgnoreCase(authenticatedUser.getRole())
                || !authenticatedUser.getUserId().equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        logger.info("Dados recebidos para atualização para player {}: {}", id, progressData);

        try {
            progressService.updateProgress(authenticatedUser, progressData);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.error("Erro na atualização do progresso: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
