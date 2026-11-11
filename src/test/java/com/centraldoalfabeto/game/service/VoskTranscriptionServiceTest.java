package com.centraldoalfabeto.game.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class VoskTranscriptionServiceTest {

    @Test
    void extraiTextoDoJsonDoVosk() {
        assertEquals("ola \"mundo\"", VoskTranscriptionService.extractText("{\n  \"text\" : \"ola \\\"mundo\\\"\"\n}"));
    }

    @Test
    void resultadoSemTextoRetornaVazio() {
        assertEquals("", VoskTranscriptionService.extractText("{\"partial\":\"a\"}"));
    }

    @Test
    void jsonInvalidoLancaErro() {
        assertThrows(IllegalStateException.class, () -> VoskTranscriptionService.extractText("nao e json"));
    }

    @Test
    void semModeloATranscricaoFalhaComEstadoIlegal() {
        VoskTranscriptionService service = new VoskTranscriptionService("");
        service.init();
        assertThrows(IllegalStateException.class, () -> service.transcribe(new byte[] {1, 2}));
    }
}
