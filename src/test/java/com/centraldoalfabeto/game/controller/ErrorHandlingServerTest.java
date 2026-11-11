package com.centraldoalfabeto.game.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.centraldoalfabeto.game.service.JwtService;
import com.centraldoalfabeto.game.service.VoskTranscriptionService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Sobe o servidor HTTP real (Tomcat) porque erros tratados pelo DefaultHandlerExceptionResolver geram um
 * dispatch de erro (/error) que o MockMvc não reproduz. Com token válido, nada disso pode virar 401.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ErrorHandlingServerTest {

    @Autowired
    private TestRestTemplate rest;
    @Autowired
    private JwtService jwtService;
    @MockitoBean
    private VoskTranscriptionService vosk;

    private HttpHeaders auth(String role) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtService.generateToken(UUID.randomUUID(), role, "x@teste.com"));
        return headers;
    }

    private void assertJsonError(ResponseEntity<String> response, int status) {
        assertEquals(status, response.getStatusCode().value(), response.getBody());
        assertTrue(response.getHeaders().getContentType().isCompatibleWith(MediaType.APPLICATION_JSON),
            "content-type: " + response.getHeaders().getContentType());
        assertTrue(response.getBody().contains("\"error\""), response.getBody());
    }

    @Test
    void parteMultipartAusenteRetorna400() {
        HttpHeaders headers = auth("STUDENT");
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("outro", "x");
        ResponseEntity<String> response = rest.exchange("/audio/transcribe", HttpMethod.POST,
            new HttpEntity<>(body, headers), String.class);
        assertJsonError(response, 400);
    }

    @Test
    void uuidInvalidoNaUrlRetorna400() {
        ResponseEntity<String> response = rest.exchange("/educators/nao-e-uuid/students", HttpMethod.GET,
            new HttpEntity<>(auth("EDUCATOR")), String.class);
        assertJsonError(response, 400);
    }

    @Test
    void rotaInexistenteComTokenRetorna404() {
        ResponseEntity<String> response = rest.exchange("/rota/que/nao/existe", HttpMethod.GET,
            new HttpEntity<>(auth("STUDENT")), String.class);
        assertJsonError(response, 404);
    }

    @Test
    void rotaInexistenteSemTokenPodeSer401() {
        ResponseEntity<String> response = rest.getForEntity("/rota/que/nao/existe", String.class);
        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    void metodoNaoSuportadoRetorna405() {
        ResponseEntity<String> response = rest.exchange("/audio/transcribe", HttpMethod.GET,
            new HttpEntity<>(auth("STUDENT")), String.class);
        assertJsonError(response, 405);
    }

    @Test
    void contentTypeNaoSuportadoRetorna415() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        ResponseEntity<String> response = rest.exchange("/players/register", HttpMethod.POST,
            new HttpEntity<>("texto", headers), String.class);
        assertJsonError(response, 415);
    }

    @Test
    void uploadMaiorQueOLimiteRetorna413() {
        HttpHeaders headers = auth("STUDENT");
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("audioFile", new ByteArrayResource(new byte[2 * 1024 * 1024]) {
            @Override
            public String getFilename() {
                return "recording.wav";
            }
        });
        ResponseEntity<String> response = rest.exchange("/audio/transcribe", HttpMethod.POST,
            new HttpEntity<>(body, headers), String.class);
        assertJsonError(response, 413);
    }

    @Test
    void jsonMalformadoRetorna400() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = rest.exchange("/auth/login", HttpMethod.POST,
            new HttpEntity<>("{nao e json", headers), String.class);
        assertJsonError(response, 400);
    }

    @Test
    void tokenInvalidoContinuaRetornando401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("token.invalido.aqui");
        ResponseEntity<String> response = rest.exchange("/audio/transcribe", HttpMethod.GET,
            new HttpEntity<>(headers), String.class);
        assertJsonError(response, 401);
    }

    @Test
    void nadaReconhecidoRetorna200ComCorpoVazio() {
        when(vosk.transcribe(any())).thenReturn("");
        HttpHeaders headers = auth("STUDENT");
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("audioFile", new ByteArrayResource(silentWav()) {
            @Override
            public String getFilename() {
                return "recording.wav";
            }
        });
        ResponseEntity<String> response = rest.exchange("/audio/transcribe", HttpMethod.POST,
            new HttpEntity<>(body, headers), String.class);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("", response.getBody() == null ? "" : response.getBody());
    }

    private static byte[] silentWav() {
        java.nio.ByteBuffer b = java.nio.ByteBuffer.allocate(44 + 3200).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        b.put("RIFF".getBytes()).putInt(36 + 3200).put("WAVE".getBytes());
        b.put("fmt ".getBytes()).putInt(16).putShort((short) 1).putShort((short) 1)
            .putInt(16000).putInt(32000).putShort((short) 2).putShort((short) 16);
        b.put("data".getBytes()).putInt(3200);
        return b.array();
    }
}
