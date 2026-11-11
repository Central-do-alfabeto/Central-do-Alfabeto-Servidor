package com.centraldoalfabeto.game.service;

import java.util.Locale;

/** Normaliza e-mails (trim + minúsculas) para cadastro, login e vínculo. */
public final class EmailNormalizer {
    private EmailNormalizer() {
    }

    public static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
