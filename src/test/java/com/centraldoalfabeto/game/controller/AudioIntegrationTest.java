package com.centraldoalfabeto.game.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.centraldoalfabeto.game.config.ApiExceptionHandler;
import com.centraldoalfabeto.game.service.JwtService;
import com.centraldoalfabeto.game.service.VoskTranscriptionService;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@SpringBootTest
@AutoConfigureMockMvc
class AudioIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;
    @MockitoBean
    private VoskTranscriptionService vosk;

    private String bearer(String role) {
        return "Bearer " + jwtService.generateToken(UUID.randomUUID(), role, "x@teste.com");
    }

    private static byte[] wav(int sampleRate, int channels, int bits, int dataBytes) {
        ByteBuffer b = ByteBuffer.allocate(44 + dataBytes).order(ByteOrder.LITTLE_ENDIAN);
        b.put("RIFF".getBytes()).putInt(36 + dataBytes).put("WAVE".getBytes());
        b.put("fmt ".getBytes()).putInt(16).putShort((short) 1).putShort((short) channels)
            .putInt(sampleRate).putInt(sampleRate * channels * bits / 8)
            .putShort((short) (channels * bits / 8)).putShort((short) bits);
        b.put("data".getBytes()).putInt(dataBytes);
        return b.array();
    }

    private MockMultipartFile file(byte[] content) {
        return new MockMultipartFile("audioFile", "recording.wav", "audio/wav", content);
    }

    @Test
    void semTokenRetorna401() throws Exception {
        mockMvc.perform(multipart("/audio/transcribe").file(file(wav(16000, 1, 16, 3200))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void alunoEEducadorPodemTranscrever() throws Exception {
        when(vosk.transcribe(any())).thenReturn("a");
        for (String role : new String[] {"STUDENT", "EDUCATOR"}) {
            mockMvc.perform(multipart("/audio/transcribe").file(file(wav(16000, 1, 16, 3200)))
                    .header("Authorization", bearer(role)))
                .andExpect(status().isOk())
                .andExpect(content().string("a"));
        }
    }

    @Test
    void arquivoQueNaoEWavRetorna415() throws Exception {
        mockMvc.perform(multipart("/audio/transcribe").file(file("isto nao e audio".getBytes()))
                .header("Authorization", bearer("STUDENT")))
            .andExpect(status().isUnsupportedMediaType())
            .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void wavComTaxaOuCanaisDiferentesRetorna415() throws Exception {
        mockMvc.perform(multipart("/audio/transcribe").file(file(wav(44100, 1, 16, 3200)))
                .header("Authorization", bearer("STUDENT")))
            .andExpect(status().isUnsupportedMediaType());
        mockMvc.perform(multipart("/audio/transcribe").file(file(wav(16000, 2, 16, 3200)))
                .header("Authorization", bearer("STUDENT")))
            .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void arquivoVazioRetorna400() throws Exception {
        mockMvc.perform(multipart("/audio/transcribe").file(file(new byte[0]))
                .header("Authorization", bearer("STUDENT")))
            .andExpect(status().isBadRequest());
    }

    @Test
    void falhaInternaNaoVazaMensagemDaExcecao() throws Exception {
        when(vosk.transcribe(any())).thenThrow(new RuntimeException("detalhe interno secreto"));
        mockMvc.perform(multipart("/audio/transcribe").file(file(wav(16000, 1, 16, 3200)))
                .header("Authorization", bearer("STUDENT")))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.error").value("Erro ao processar áudio."))
            .andExpect(content().string(org.hamcrest.Matchers.not(
                org.hamcrest.Matchers.containsString("secreto"))));
    }

    @Test
    void modeloIndisponivelRetorna503() throws Exception {
        when(vosk.transcribe(any())).thenThrow(new IllegalStateException("Modelo Vosk não foi inicializado."));
        mockMvc.perform(multipart("/audio/transcribe").file(file(wav(16000, 1, 16, 3200)))
                .header("Authorization", bearer("STUDENT")))
            .andExpect(status().isServiceUnavailable());
    }

    @Test
    void uploadGrandeDemaisRetorna413EmJson() {
        var response = new ApiExceptionHandler().handleTooLarge(new MaxUploadSizeExceededException(1024 * 1024));
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.getStatusCode());
        assertEquals("Arquivo grande demais", response.getBody().get("error"));
    }
}
