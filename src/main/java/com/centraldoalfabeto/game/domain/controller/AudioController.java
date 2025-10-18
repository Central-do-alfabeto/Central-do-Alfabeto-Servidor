package com.centraldoalfabeto.game.domain.controller;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.RecognitionConfig.AudioEncoding;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.SpeechRecognitionResult;
import com.google.cloud.speech.v1.SpeechSettings;
import com.google.protobuf.ByteString;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/audio")
public class AudioController {
    @PostMapping("/transcribe")
    public ResponseEntity<String> transcribeAudio(@RequestParam("audioFile") MultipartFile audioFile) {
        if (audioFile.isEmpty()) {
            return ResponseEntity.badRequest().body("Nenhum áudio enviado.");
        }

        try (SpeechClient speechClient = createSpeechClient()) {
            ByteString audioBytes = ByteString.copyFrom(audioFile.getBytes());

            RecognitionConfig config = RecognitionConfig.newBuilder()
                    .setEncoding(AudioEncoding.WEBM_OPUS)
                    .setSampleRateHertz(48000)
                    .setLanguageCode("pt-BR")
                    .build();

            RecognitionAudio audio = RecognitionAudio.newBuilder()
                    .setContent(audioBytes)
                    .build();

            com.google.cloud.speech.v1.RecognizeResponse response = speechClient.recognize(config, audio);
            
            StringBuilder transcript = new StringBuilder();
            for (SpeechRecognitionResult result : response.getResultsList()) {
                SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
                transcript.append(alternative.getTranscript());
            }

            return ResponseEntity.ok(transcript.toString());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao processar o áudio enviado.");
        }
    }

    private SpeechClient createSpeechClient() throws IOException {
        SpeechSettings.Builder settingsBuilder = SpeechSettings.newBuilder();

        GoogleCredentials credentials = resolveCredentials();
        if (credentials != null) {
            settingsBuilder.setCredentialsProvider(FixedCredentialsProvider.create(credentials));
        }

        try {
            return SpeechClient.create(settingsBuilder.build());
        } catch (IOException ioEx) {
            throw ioEx;
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Credenciais do Google Cloud Speech não configuradas. " +
                            "Defina GOOGLE_APPLICATION_CREDENTIALS, GOOGLE_APPLICATION_CREDENTIALS_JSON " +
                            "ou GOOGLE_APPLICATION_CREDENTIALS_BASE64.",
                    ex);
        }
    }

    private GoogleCredentials resolveCredentials() throws IOException {
        String base64Credentials = System.getenv("GOOGLE_APPLICATION_CREDENTIALS_BASE64");
        if (base64Credentials != null && !base64Credentials.isBlank()) {
            byte[] decoded = Base64.getDecoder().decode(base64Credentials);
            return GoogleCredentials.fromStream(new ByteArrayInputStream(decoded));
        }

        String jsonCredentials = System.getenv("GOOGLE_APPLICATION_CREDENTIALS_JSON");
        if (jsonCredentials != null && !jsonCredentials.isBlank()) {
            byte[] bytes = jsonCredentials.getBytes(StandardCharsets.UTF_8);
            return GoogleCredentials.fromStream(new ByteArrayInputStream(bytes));
        }

        String credentialsPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
        if (credentialsPath != null && !credentialsPath.isBlank()) {
            Path path = Path.of(credentialsPath);
            try (InputStream inputStream = Files.newInputStream(path)) {
                return GoogleCredentials.fromStream(inputStream);
            }
        }

        return null;
    }
}
