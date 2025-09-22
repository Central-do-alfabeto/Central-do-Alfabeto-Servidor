package com.centraldoalfabeto.game.domain.controller;

import com.centraldoalfabeto.game.domain.model.Jogador;
import com.centraldoalfabeto.game.domain.model.TotalErrors;
import com.centraldoalfabeto.game.domain.model.TotalAudioReproductions;
import com.centraldoalfabeto.game.repository.JogadorRepository;
import com.centraldoalfabeto.game.repository.TotalErrorsRepository;
import com.centraldoalfabeto.game.repository.TotalAudioReproductionsRepository;
import com.centraldoalfabeto.game.dto.ProgressUpdateDTO;
import org.springframework.security.crypto.password.PasswordEncoder;
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

    @Autowired
    private TotalErrorsRepository totalErrorsRepository;

    @Autowired
    private TotalAudioReproductionsRepository totalAudioReproductionsRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @PostMapping("/register")
    public ResponseEntity<Jogador> registerPlayer(@RequestBody Jogador player) {
        if (player.getFullName() == null || player.getEmail() == null || player.getPassword() == null) {
            return ResponseEntity.badRequest().build();
        }

        Optional<Jogador> existingPlayer = jogadorRepository.findByEmail(player.getEmail());
        if (existingPlayer.isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        player.setPassword(passwordEncoder.encode(player.getPassword()));

        Jogador newPlayer = jogadorRepository.save(player);
        return ResponseEntity.status(HttpStatus.CREATED).body(newPlayer);
    }

    @PutMapping("/{id}/updateProgress")
    public ResponseEntity<Void> updateProgress(
            @PathVariable Long id,
            @RequestBody ProgressUpdateDTO progressData) {

        logger.info("Dados recebidos para atualização: {}", progressData);

        Optional<Jogador> optionalPlayer = jogadorRepository.findById(id);
        if (optionalPlayer.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Jogador player = optionalPlayer.get();

        Integer currentPhaseIndex = progressData.getCurrentPhaseIndex();
        Integer numberOfErrors = progressData.getNumberOfErrors();
        Integer numberOfSoundRepeats = progressData.getNumberOfSoundRepeats();

        if (currentPhaseIndex == null || numberOfErrors == null || numberOfSoundRepeats == null) {
            return ResponseEntity.badRequest().build();
        }
        
        TotalErrors totalErrors = new TotalErrors();
        totalErrors.setJogador(player);
        totalErrors.setCurrentPhaseIndex(currentPhaseIndex);
        totalErrors.setValue(numberOfErrors);
        totalErrorsRepository.save(totalErrors);

        TotalAudioReproductions totalAudioReproductions = new TotalAudioReproductions();
        totalAudioReproductions.setJogador(player);
        totalAudioReproductions.setCurrentPhaseIndex(currentPhaseIndex);
        totalAudioReproductions.setValue(numberOfSoundRepeats);
        totalAudioReproductionsRepository.save(totalAudioReproductions);
        
        player.setCurrentPhaseIndex(currentPhaseIndex);
        
        jogadorRepository.save(player);
        return ResponseEntity.noContent().build();
    }
}
