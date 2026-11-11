package com.centraldoalfabeto.game.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.centraldoalfabeto.game.service.JogadorService;
import com.centraldoalfabeto.game.service.JwtService;
import com.centraldoalfabeto.game.service.ProgressService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Simula violação de unicidade em cadastro/progresso concorrentes: deve virar 409, nunca 500. */
@SpringBootTest
@AutoConfigureMockMvc
class ConflictHandlingTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;
    @MockitoBean
    private JogadorService jogadorService;
    @MockitoBean
    private ProgressService progressService;

    @Test
    void cadastroConcorrenteViraConflito() throws Exception {
        when(jogadorService.registerPlayer(any())).thenThrow(new DataIntegrityViolationException("uk_users_email"));

        mockMvc.perform(post("/players/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Aluno\",\"email\":\"c@teste.com\",\"senha\":\"senha1234\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void progressoDuplicadoConcorrenteViraConflito() throws Exception {
        when(progressService.registerProgress(any(), any()))
            .thenThrow(new DataIntegrityViolationException("uk_player_data_player_phase"));
        UUID id = UUID.randomUUID();
        String token = jwtService.generateToken(id, "STUDENT", "a@teste.com");

        mockMvc.perform(post("/players/" + id + "/progress")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPhaseIndex\":0,\"errorsData\":0,\"soundRepeatsData\":0}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").exists());
    }
}
