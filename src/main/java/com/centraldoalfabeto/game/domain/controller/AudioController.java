package com.centraldoalfabeto.game.domain.controller;

import com.centraldoalfabeto.game.service.VoskTranscriptionService;
import com.centraldoalfabeto.game.service.WavAudio;
import java.io.IOException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Transcrição de fala. Qualquer usuário autenticado (aluno ou educador) pode chamar. */
@RestController
@RequestMapping("/audio")
public class AudioController {
    private static final Logger logger = LoggerFactory.getLogger(AudioController.class);

    private final VoskTranscriptionService voskService;

    public AudioController(VoskTranscriptionService voskService) {
        this.voskService = voskService;
    }

    @PostMapping("/transcribe")
    public ResponseEntity<?> transcribeAudio(@RequestParam("audioFile") MultipartFile audioFile) {
        if (audioFile.isEmpty()) {
            return error(HttpStatus.BAD_REQUEST, "Nenhum áudio enviado.");
        }

        WavAudio wav;
        try {
            wav = WavAudio.parse(audioFile.getBytes());
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, e.getMessage());
        } catch (IOException e) {
            logger.error("Falha ao ler o áudio enviado", e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao processar áudio.");
        }

        try {
            String transcript = voskService.transcribe(wav.pcm());
            return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(transcript);
        } catch (IllegalStateException e) {
            logger.error("Transcrição indisponível ou falhou", e);
            return error(HttpStatus.SERVICE_UNAVAILABLE, "Reconhecimento de fala indisponível no momento.");
        } catch (Exception e) {
            logger.error("Erro na transcrição Vosk", e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao processar áudio.");
        }
    }

    private static ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(Map.of("error", message));
    }
}
