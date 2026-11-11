package com.centraldoalfabeto.game.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.centraldoalfabeto.game.domain.model.User;
import com.centraldoalfabeto.game.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LoginRoleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private void saveUser(String email, String metadados) {
        User user = new User();
        user.setNome("Usuario");
        user.setEmail(email);
        user.setSenhaHash(passwordEncoder.encode("senha1234"));
        user.setMetadados(metadados);
        userRepository.save(user);
    }

    private String body(String email, String extra) {
        return "{\"email\":\"" + email + "\",\"password\":\"senha1234\"" + extra + "}";
    }

    @Test
    void isStudentEnviadoPeloClienteEIgnorado() throws Exception {
        saveUser("edu@teste.com", "{\"role\":\"educator\"}");

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(body("edu@teste.com", ",\"isStudent\":true")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("EDUCATOR"))
            .andExpect(jsonPath("$.isStudent").value(false));

        saveUser("aluno@teste.com", "{\"role\":\"student\"}");

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(body("aluno@teste.com", ",\"isStudent\":false")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("STUDENT"))
            .andExpect(jsonPath("$.isStudent").value(true));
    }

    @Test
    void contaSemPapelNosMetadadosNaoFazLogin() throws Exception {
        saveUser("semrole@teste.com", null);

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(body("semrole@teste.com", ",\"isStudent\":false")))
            .andExpect(status().isUnauthorized());
    }
}
