package com.centraldoalfabeto.game.domain.controller;

import com.centraldoalfabeto.game.domain.model.Jogador;
import com.centraldoalfabeto.game.repository.JogadorRepository; // "Supondo que você use o repositório diretamente" <- não sei o que o Gemini quis dizer aqui, talvez tenha a ver com o Service
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
        player.setCurrentPhaseIndex(currentPhaseIndex);
        player.setNumberOfErrors(numberOfErrors);
        player.setNumberOfSoundRepeats(numberOfSoundRepeats);

        Jogador updatedPlayer = jogadorRepository.save(player);
        return new ResponseEntity<>(updatedPlayer, HttpStatus.OK);
    }
}
