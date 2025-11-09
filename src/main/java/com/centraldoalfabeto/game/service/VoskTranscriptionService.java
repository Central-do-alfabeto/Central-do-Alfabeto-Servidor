package com.centraldoalfabeto.game.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.ClassPathResource;

import org.vosk.Model;
import org.vosk.LogLevel;
import org.vosk.Recognizer;
import org.vosk.LibVosk;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.File;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

@Service
public class VoskTranscriptionService {
    private Model model;
    
    private static final String MODEL_FOLDER_NAME = "vosk-model"; 

    @PostConstruct
    public void init() {
        try {
            LibVosk.setLogLevel(LogLevel.DEBUG);
            System.out.println("Tentando carregar modelo Vosk...");

            ClassPathResource resource = new ClassPathResource(MODEL_FOLDER_NAME);
            
            File modelFile = resource.getFile();
            String path = modelFile.getAbsolutePath();

            System.out.println("Tentando carregar modelo Vosk de: " + path);
            
            model = new Model(path);
            System.out.println("✅ Modelo Vosk carregado com sucesso!");       
        } catch (Exception e) {
            System.err.println("❌ FALHA CRÍTICA ao carregar o modelo Vosk. Verifique:");
            System.err.println("1. Se a pasta 'vosk-model' está diretamente em src/main/resources.");
            System.err.println("2. Se o projeto foi compilado (Build) corretamente para que o modelo esteja na pasta 'target/classes'.");
            e.printStackTrace();
            model = null;
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
            throw new IllegalStateException("Modelo Vosk não foi inicializado corretamente.");
        }
        
        try (InputStream ais = new BufferedInputStream(audioFile.getInputStream());
             AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(ais);
             Recognizer recognizer = new Recognizer(model, 16000.0f)) {
            audioInputStream.skip(44); 

            String finalResult = "";
            byte[] buffer = new byte[4096];
            int bytesRead;
            
            while ((bytesRead = audioInputStream.read(buffer)) != -1) {
                recognizer.acceptWaveForm(buffer, bytesRead);
            }
            
            finalResult = recognizer.getResult(); 

            if (finalResult.contains("\"text\"")) {
                int textStart = finalResult.indexOf("\"text\" : \"");
                if (textStart != -1) {
                    int contentStart = textStart + 10; 
                    int contentEnd = finalResult.indexOf("\"", contentStart);
                    if (contentEnd != -1) {
                        return finalResult.substring(contentStart, contentEnd).trim();
                    }
                }
            }
            
            return ""; 
        } catch (Exception e) {
            System.err.println("❌ Erro ao transcrever áudio. Verifique se o arquivo enviado pelo Frontend é WAV 16kHz.");
            e.printStackTrace();
            throw new Exception("Erro de processamento de áudio/Vosk: " + e.getMessage());
        }
    }
}