package com.centraldoalfabeto.game.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;

@Service
public class VoskTranscriptionService {
    private static final Logger logger = LoggerFactory.getLogger(VoskTranscriptionService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String modelPath;
    private Model model;

    public VoskTranscriptionService(@Value("${vosk.model-path:vosk-model}") String modelPath) {
        this.modelPath = modelPath;
    }

    @PostConstruct
    public void init() {
        if (modelPath == null || modelPath.isBlank()) {
            logger.info("Caminho do modelo Vosk vazio: reconhecimento de fala desabilitado.");
            return;
        }
        try {
            org.vosk.LibVosk.setLogLevel(LogLevel.WARNINGS);
            // O caminho é relativo ao diretório de execução (padrão: vosk-model).
            logger.info("Carregando modelo Vosk de {}", new java.io.File(modelPath).getAbsolutePath());
            model = new Model(modelPath);
            logger.info("Modelo Vosk carregado.");
        } catch (Exception | LinkageError e) {
            logger.error("Falha ao carregar o modelo Vosk; o reconhecimento de fala ficará indisponível.", e);
        }
    }

    @PreDestroy
    public void destroy() {
        if (model != null) {
            model.close();
            logger.info("Modelo Vosk liberado.");
        }
    }

    /** Transcreve dados PCM 16-bit mono 16 kHz. Lança IllegalStateException se o modelo não estiver carregado. */
    public String transcribe(byte[] pcm) {
        if (model == null) {
            throw new IllegalStateException("Modelo Vosk não foi inicializado.");
        }

        try (Recognizer recognizer = new Recognizer(model, WavAudio.SAMPLE_RATE)) {
            int chunk = 4096;
            for (int offset = 0; offset < pcm.length; offset += chunk) {
                int length = Math.min(chunk, pcm.length - offset);
                byte[] part = java.util.Arrays.copyOfRange(pcm, offset, offset + length);
                recognizer.acceptWaveForm(part, length);
            }
            return extractText(recognizer.getFinalResult());
        } catch (IOException e) {
            throw new IllegalStateException("Erro ao transcrever áudio.", e);
        }
    }

    static String extractText(String resultJson) {
        try {
            JsonNode text = MAPPER.readTree(resultJson).path("text");
            return text.isTextual() ? text.asText() : "";
        } catch (IOException e) {
            throw new IllegalStateException("Resposta inesperada do reconhecedor.", e);
        }
    }
}
