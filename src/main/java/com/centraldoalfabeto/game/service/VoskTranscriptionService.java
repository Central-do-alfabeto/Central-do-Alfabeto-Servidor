package com.centraldoalfabeto.game.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.vosk.Model;
import org.vosk.Recognizer;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.io.BufferedInputStream;
import java.io.InputStream;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

@Service
public class VoskTranscriptionService {
    private Model model;

    @PostConstruct
    public void init() {
        try {
            org.vosk.LibVosk.setLogLevel(org.vosk.LogLevel.DEBUG);
            System.out.println("Tentando carregar modelo Vosk...");

            String absoluteVoskPath = "C:/Users/Brian/Documents/workspace-spring-tools-for-eclipse-4.31.0.RELEASE/Central-do-Alfabeto-Servidor/src/main/resources/vosk-model"; 
            
            System.out.println("Tentando carregar modelo Vosk de: " + absoluteVoskPath);
            
            model = new Model(absoluteVoskPath);
            System.out.println("✅ Modelo Vosk carregado com sucesso!");
            
        } catch (Exception e) {
            System.err.println("❌ FALHA CRÍTICA ao carregar o modelo Vosk. (Verifique o caminho fixo!)");
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