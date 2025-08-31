import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.SpeechRecognitionResult;
import com.google.protobuf.ByteString;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@RestController
@RequestMapping("/audio")
public class AudioController {
    @PostMapping("/transcribe")
    public ResponseEntity<String> transcribeAudio(@RequestParam("audioFile") MultipartFile audioFile) {
        try (SpeechClient speechClient = SpeechClient.create()) {
            // Lê o áudio
            ByteString audioBytes = ByteString.copyFrom(audioFile.getBytes());

            // Configura o reconhecimento de fala para português
            RecognitionConfig config = RecognitionConfig.newBuilder()
                    .setLanguageCode("pt-BR")
                    .build();

            // Configura o áudio
            RecognitionAudio audio = RecognitionAudio.newBuilder()
                    .setContent(audioBytes)
                    .build();

            // Faz o pedido para a API do Google
            com.google.cloud.speech.v1.RecognizeResponse response = speechClient.recognize(config, audio);
            
            // Extrai o texto da resposta
            StringBuilder transcript = new StringBuilder();
            for (SpeechRecognitionResult result : response.getResultsList()) {
                SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
                transcript.append(alternative.getTranscript());
            }

            return ResponseEntity.ok(transcript.toString());
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing the audio.");
        }
    }
}
