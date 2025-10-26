package com.centraldoalfabeto.game.domain.controller;

import com.centraldoalfabeto.game.service.VoskTranscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/audio")
public class AudioController {
    private final VoskTranscriptionService voskService;

    public AudioController(VoskTranscriptionService voskService) {
        this.voskService = voskService;
    }

    @PostMapping("/transcribe")
    public ResponseEntity<String> transcribeAudio(@RequestParam("audioFile") MultipartFile audioFile) {
        if (audioFile.isEmpty()) {
            return ResponseEntity.badRequest().body("Nenhum áudio enviado.");
        }

        try {
            String transcript = voskService.transcribe(audioFile);
            
            return ResponseEntity.ok(transcript); 
        } catch (Exception e) {
            System.err.println("Erro na transcrição Vosk: " + e.getMessage());
            return ResponseEntity.internalServerError().body("Erro ao processar áudio: " + e.getMessage());
        }
    }
}