package com.centraldoalfabeto.game.domain.controller;

import com.centraldoalfabeto.game.domain.model.Jogador;
import com.centraldoalfabeto.game.repository.JogadorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;

@RestController
@RequestMapping("/players")
public class JogadorController {
    @Autowired
    private JogadorRepository jogadorRepository;

    @PostMapping("/register")
    public ResponseEntity<Jogador> registerPlayer(@RequestBody Jogador player) {
        Jogador newPlayer = jogadorRepository.save(player);
        return new ResponseEntity<>(newPlayer, HttpStatus.CREATED);
    }

    @PutMapping("/{id}/updateProgress")
    public ResponseEntity<Jogador> updateProgress(
            @PathVariable Long id,
            @RequestParam Integer currentPhaseIndex,
            @RequestParam Integer numberOfErrors,
            @RequestParam Integer numberOfSoundRepeats) {

        Optional<Jogador> optionalPlayer = jogadorRepository.findById(id);
        if (optionalPlayer.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Jogador player = optionalPlayer.get();

        if (currentPhaseIndex < 0 || currentPhaseIndex >= player.getNumberOfErrorsByPhase().length) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        player.setCurrentPhaseIndex(currentPhaseIndex);
        
        player.getNumberOfErrorsByPhase()[currentPhaseIndex] = numberOfErrors;
        player.getNumberOfSoundRepeatsByPhase()[currentPhaseIndex] = numberOfSoundRepeats;

        Jogador updatedPlayer = jogadorRepository.save(player);
        return new ResponseEntity<>(updatedPlayer, HttpStatus.OK);
    }
}
