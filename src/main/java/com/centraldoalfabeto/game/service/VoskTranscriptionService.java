package com.centraldoalfabeto.game.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.vosk.Model;
import org.vosk.LogLevel;
import org.vosk.Recognizer;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.BufferedInputStream;
import java.io.InputStream;

@Service
public class VoskTranscriptionService {
    private Model model;

    private static final String MODEL_PATH = "vosk-model"; 

    @PostConstruct
    public void init() {
        try {
        	org.vosk.LibVosk.setLogLevel(LogLevel.DEBUG);
        	System.out.println("Tentando carregar modelo Vosk...");
        	
            InputStream modelStream = getClass().getClassLoader().getResourceAsStream(MODEL_PATH);
            if (modelStream == null) {
                throw new RuntimeException("Modelo Vosk não encontrado no classpath: " + MODEL_PATH);
            }
            
            String absolutePath = new java.io.File(MODEL_PATH).getAbsolutePath();
            System.out.println("Tentando carregar modelo Vosk de: " + absolutePath);

            model = new Model(MODEL_PATH);
            System.out.println("Modelo Vosk carregado com sucesso!");
        } catch (Exception e) {
            System.err.println("Falha ao carregar o modelo Vosk:");
            e.printStackTrace();
        }
    }

    @PreDestroy
    public void destroy() {
        if (model != null) {
            model.close();
            System.out.println("Modelo Vosk liberado.");
        }
    }

    public String transcribe(MultipartFile audioFile) throws Exception {
        if (model == null) {
            throw new IllegalStateException("Modelo Vosk não foi inicializado.");
        }
        
        try (InputStream ais = new BufferedInputStream(audioFile.getInputStream());
             Recognizer recognizer = new Recognizer(model, 16000)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            
            while ((bytesRead = ais.read(buffer)) != -1) {
                recognizer.acceptWaveForm(buffer, bytesRead);
            }
            
            String finalResult = recognizer.getFinalResult(); // Corrigido: getFinalResult() em vez de getResult()

            int textStart = finalResult.indexOf("\"text\" : \"");
            if (textStart != -1) {
                int contentStart = textStart + 10; 
                int contentEnd = finalResult.indexOf("\"", contentStart);
                return finalResult.substring(contentStart, contentEnd);
            }
            
            return finalResult;
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Erro ao transcrever áudio com Vosk. Verifique se o formato é WAV 16kHz.");
        }
    }
}