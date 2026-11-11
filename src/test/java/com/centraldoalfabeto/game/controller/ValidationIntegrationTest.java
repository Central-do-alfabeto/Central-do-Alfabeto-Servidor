package com.centraldoalfabeto.game.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.centraldoalfabeto.game.domain.model.User;
import com.centraldoalfabeto.game.repository.UserRepository;
import com.centraldoalfabeto.game.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ValidationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtService jwtService;

    @Test
    void cadastroDeAlunoComDadosInvalidosListaOsCampos() throws Exception {
        mockMvc.perform(post("/players/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"\",\"email\":\"nao-e-email\",\"senha\":\"123\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fields.nome").exists())
            .andExpect(jsonPath("$.fields.email").exists())
            .andExpect(jsonPath("$.fields.senha").exists());
    }

    @Test
    void cadastroDeEducadorSemCamposObrigatorios() throws Exception {
        mockMvc.perform(post("/educators/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fields.nome").exists())
            .andExpect(jsonPath("$.fields.email").exists())
            .andExpect(jsonPath("$.fields.senha").exists());
    }

    @Test
    void cadastroValidoRetorna201() throws Exception {
        mockMvc.perform(post("/players/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Aluno Valido\",\"email\":\"valido@teste.com\",\"senha\":\"senha1234\"}"))
            .andExpect(status().isCreated());
    }

    @Test
    void loginSemCredenciaisRetorna400() throws Exception {
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fields.email").exists())
            .andExpect(jsonPath("$.fields.password").exists());
    }

    @Test
    void corpoMalformadoRetorna400EmJson() throws Exception {
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{nao e json"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void progressoComValoresNegativosRetorna400() throws Exception {
        User student = new User();
        student.setNome("Aluno");
        student.setEmail("prog@teste.com");
        student.setSenhaHash("hash");
        student.setMetadados("{\"role\":\"student\"}");
        student = userRepository.save(student);
        String token = jwtService.generateToken(student.getId(), "STUDENT", student.getEmail());

        mockMvc.perform(post("/players/" + student.getId() + "/progress")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPhaseIndex\":-1,\"errorsData\":-5,\"soundRepeatsData\":0}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fields.currentPhaseIndex").exists())
            .andExpect(jsonPath("$.fields.errorsData").exists());
    }

    @Test
    void vinculoDeAlunoComEmailInvalidoRetorna400() throws Exception {
        User educator = new User();
        educator.setNome("Edu");
        educator.setEmail("edu-val@teste.com");
        educator.setSenhaHash("hash");
        educator.setMetadados("{\"role\":\"educator\"}");
        educator = userRepository.save(educator);
        String token = jwtService.generateToken(educator.getId(), "EDUCATOR", educator.getEmail());

        mockMvc.perform(post("/educators/" + educator.getId() + "/students")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"x\",\"studentName\":\" \"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fields.email").exists())
            .andExpect(jsonPath("$.fields.studentName").exists());
    }

    @Test
    void progressoAcimaDoLimiteOuComSaltoRetorna400() throws Exception {
        User student = new User();
        student.setNome("Aluno");
        student.setEmail("limite@teste.com");
        student.setSenhaHash("hash");
        student.setMetadados("{\"role\":\"student\"}");
        student = userRepository.save(student);
        String token = "Bearer " + jwtService.generateToken(student.getId(), "STUDENT", student.getEmail());
        String url = "/players/" + student.getId() + "/progress";

        mockMvc.perform(post(url).header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPhaseIndex\":26}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fields.currentPhaseIndex").exists());

        mockMvc.perform(post(url).header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPhaseIndex\":3}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").exists());

        mockMvc.perform(post(url).header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPhaseIndex\":0}"))
            .andExpect(status().isCreated());
        mockMvc.perform(post(url).header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPhaseIndex\":1}"))
            .andExpect(status().isCreated());
    }
}
