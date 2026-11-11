package com.centraldoalfabeto.game.domain.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

/** Papel do usuário, lido de forma estruturada do JSON da coluna metadados ({"role": "student"|"educator"}). */
public enum UserRole {
    STUDENT, EDUCATOR;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Único ponto de leitura do papel. Retorna null se os metadados forem nulos, inválidos ou sem papel reconhecido. */
    public static UserRole fromMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return null;
        }
        try {
            JsonNode role = MAPPER.readTree(metadataJson).path("role");
            if (!role.isTextual()) {
                return null;
            }
            switch (role.asText().trim().toLowerCase(java.util.Locale.ROOT)) {
                case "student":
                case "aluno":
                case "player":
                    return STUDENT;
                case "educator":
                case "educador":
                    return EDUCATOR;
                default:
                    return null;
            }
        } catch (IOException e) {
            return null;
        }
    }

    public static UserRole of(User user) {
        return user == null ? null : fromMetadata(user.getMetadados());
    }
}
