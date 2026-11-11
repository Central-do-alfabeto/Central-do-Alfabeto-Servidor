package com.centraldoalfabeto.game.config;

import java.util.Map;

/** Corpo padrão de erro: {"error": "mensagem"}. */
public final class ApiErrors {
    private ApiErrors() {
    }

    public static Map<String, String> of(String message) {
        return Map.of("error", message);
    }
}
