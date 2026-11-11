package com.centraldoalfabeto.game.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void falhaInesperadaRetorna500GenericoSemDetalhes() throws Exception {
        var response = handler.handleUnexpected(new RuntimeException("detalhe interno secreto"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(response.getBody().toString().contains("secreto"));
    }

    @Test
    void excecoesDeSegurancaNaoViram500() {
        assertThrows(AccessDeniedException.class,
            () -> handler.handleUnexpected(new AccessDeniedException("negado")));
    }
}
