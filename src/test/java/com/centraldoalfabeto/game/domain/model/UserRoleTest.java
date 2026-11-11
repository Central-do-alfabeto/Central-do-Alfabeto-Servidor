package com.centraldoalfabeto.game.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class UserRoleTest {

    @Test
    void reconheceOsPapeisPeloCampoRole() {
        assertEquals(UserRole.STUDENT, UserRole.fromMetadata("{\"role\":\"student\"}"));
        assertEquals(UserRole.STUDENT, UserRole.fromMetadata("{\"role\":\"Aluno\"}"));
        assertEquals(UserRole.EDUCATOR, UserRole.fromMetadata("{ \"role\" : \"EDUCATOR\" }"));
        assertEquals(UserRole.EDUCATOR, UserRole.fromMetadata("{\"role\":\"educador\"}"));
    }

    @Test
    void naoConfundeTextoSoltoComPapel() {
        assertEquals(UserRole.STUDENT, UserRole.fromMetadata("{\"nota\":\"educator\",\"role\":\"student\"}"));
        assertNull(UserRole.fromMetadata("{\"nota\":\"educator\"}"));
        assertNull(UserRole.fromMetadata("{\"role\":\"educator-fake\"}"));
        assertNull(UserRole.fromMetadata("{\"role\":123}"));
    }

    @Test
    void metadadosAusentesOuInvalidosNaoTemPapel() {
        assertNull(UserRole.fromMetadata(null));
        assertNull(UserRole.fromMetadata(" "));
        assertNull(UserRole.fromMetadata("nao e json"));
        assertNull(UserRole.of(null));
    }
}
