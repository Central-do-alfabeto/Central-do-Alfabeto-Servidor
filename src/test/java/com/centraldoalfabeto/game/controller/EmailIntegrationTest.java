package com.centraldoalfabeto.game.controller;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.centraldoalfabeto.game.domain.model.PlayerData;
import com.centraldoalfabeto.game.domain.model.User;
import com.centraldoalfabeto.game.repository.PlayerDataRepository;
import com.centraldoalfabeto.game.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EmailIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PlayerDataRepository playerDataRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String registro(String email) {
        return "{\"nome\":\"Aluno\",\"email\":\"" + email + "\",\"senha\":\"senha1234\"}";
    }

    @Test
    void cadastroNormalizaEmailEBloqueiaDuplicataComOutraCaixa() throws Exception {
        mockMvc.perform(post("/players/register").contentType(MediaType.APPLICATION_JSON)
                .content(registro("Aluno.Caixa@Teste.COM")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value("aluno.caixa@teste.com"));

        mockMvc.perform(post("/educators/register").contentType(MediaType.APPLICATION_JSON)
                .content(registro("ALUNO.CAIXA@teste.com")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void loginAceitaEmailComOutraCaixaEEspacos() throws Exception {
        mockMvc.perform(post("/players/register").contentType(MediaType.APPLICATION_JSON)
                .content(registro("login.caixa@teste.com")))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\" Login.Caixa@TESTE.com \",\"password\":\"senha1234\"}"))
            .andExpect(status().isOk());
    }

    @Test
    void bancoRecusaEmailComMaiusculasEDuplicado() {
        UUID id = UUID.randomUUID();
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
            "INSERT INTO users (id, email) VALUES (?, ?)", id, "Maiusculo@Teste.com"));
    }

    @Test
    void bancoRecusaProgressoDuplicadoNaMesmaFase() {
        User user = new User();
        user.setNome("Aluno");
        user.setEmail("fase@teste.com");
        user.setMetadados("{\"role\":\"student\"}");
        final UUID userId = userRepository.save(user).getId();

        playerDataRepository.saveAndFlush(snapshot(userId, 2));
        assertThrows(DataIntegrityViolationException.class,
            () -> playerDataRepository.saveAndFlush(snapshot(userId, 2)));
    }

    private static PlayerData snapshot(UUID playerId, int phase) {
        PlayerData data = new PlayerData();
        data.setPlayerId(playerId);
        data.setPhaseIndex(phase);
        return data;
    }
}
