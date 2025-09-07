package com.centraldoalfabeto.game.domain.controller;

import com.centraldoalfabeto.game.domain.model.Jogador;
import com.centraldoalfabeto.game.repository.JogadorRepository;
import com.centraldoalfabeto.game.dto.ProgressUpdateDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/players")
public class JogadorController {
	private static final Logger logger = LoggerFactory.getLogger(JogadorController.class);

    @Autowired
    private JogadorRepository jogadorRepository;

    @PostMapping("/register")
    public ResponseEntity<Jogador> registerPlayer(@RequestBody Jogador player) {
        if (player.getFullName() == null || player.getEmail() == null || player.getPassword() == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        Jogador newPlayer = jogadorRepository.save(player);
        return new ResponseEntity<>(newPlayer, HttpStatus.CREATED);
    }
    
    @PutMapping("/{id}/updateProgress")
    public ResponseEntity<Void> updateProgress(
            @PathVariable Long id,
            @RequestBody ProgressUpdateDTO progressData) {
    	logger.info("Dados recebidos para atualização: {}", progressData);
        logger.info("currentPhaseIndex: {}", progressData.getCurrentPhaseIndex());
        logger.info("numberOfErrors: {}", progressData.getNumberOfErrors());
        logger.info("numberOfSoundRepeats: {}", progressData.getNumberOfSoundRepeats());
        
        Optional<Jogador> optionalPlayer = jogadorRepository.findById(id);
        if (optionalPlayer.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Jogador player = optionalPlayer.get();

        Integer currentPhaseIndex = progressData.getCurrentPhaseIndex();
        Integer numberOfErrors = progressData.getNumberOfErrors();
        Integer numberOfSoundRepeats = progressData.getNumberOfSoundRepeats();

        if (currentPhaseIndex == null || numberOfErrors == null || numberOfSoundRepeats == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        if (currentPhaseIndex < 0 || currentPhaseIndex >= player.getNumberOfErrorsByPhase().length) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        player.setCurrentPhaseIndex(currentPhaseIndex);
        player.getNumberOfErrorsByPhase()[currentPhaseIndex] = numberOfErrors;
        player.getNumberOfSoundRepeatsByPhase()[currentPhaseIndex] = numberOfSoundRepeats;

        jogadorRepository.save(player);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
