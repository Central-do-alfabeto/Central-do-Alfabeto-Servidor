package com.centraldoalfabeto.game.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.centraldoalfabeto.game.domain.model.EducatorStudentLink;
import com.centraldoalfabeto.game.domain.model.User;
import com.centraldoalfabeto.game.repository.EducatorStudentLinkRepository;
import com.centraldoalfabeto.game.repository.UserRepository;
import com.centraldoalfabeto.game.service.JwtService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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
class AuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EducatorStudentLinkRepository linkRepository;
    @Autowired
    private JwtService jwtService;

    private User educatorA;
    private User educatorB;
    private User studentOfA;
    private User otherStudent;

    @BeforeEach
    void setUp() {
        educatorA = saveUser("Educadora A", "a@teste.com", "educator");
        educatorB = saveUser("Educador B", "b@teste.com", "educator");
        studentOfA = saveUser("Aluno Um", "aluno1@teste.com", "student");
        otherStudent = saveUser("Aluno Dois", "aluno2@teste.com", "student");
        linkRepository.save(new EducatorStudentLink(educatorA.getId(), studentOfA.getId()));
    }

    private User saveUser(String nome, String email, String role) {
        User user = new User();
        user.setNome(nome);
        user.setEmail(email);
        user.setSenhaHash("hash");
        user.setMetadados("{\"role\":\"" + role + "\"}");
        return userRepository.save(user);
    }

    private String bearer(User user, String role) {
        return "Bearer " + jwtService.generateToken(user.getId(), role, user.getEmail());
    }

    private static String json(UUID... ids) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < ids.length; i++) {
            sb.append(i > 0 ? "," : "").append('"').append(ids[i]).append('"');
        }
        return sb.append(']').toString();
    }

    @Test
    void semTokenRetorna401EmJson() throws Exception {
        mockMvc.perform(get("/educators/" + educatorA.getId() + "/students"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void tokenInvalidoRetorna401() throws Exception {
        mockMvc.perform(get("/educators/" + educatorA.getId() + "/students")
                .header("Authorization", "Bearer token.invalido.aqui"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenExpiradoRetorna401() throws Exception {
        javax.crypto.SecretKey key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(
            "segredo-de-teste-com-mais-de-32-caracteres-fixos".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String token = io.jsonwebtoken.Jwts.builder().setSubject(educatorA.getId().toString())
            .setExpiration(new java.util.Date(System.currentTimeMillis() - 60_000))
            .claim("role", "EDUCATOR").signWith(key).compact();
        mockMvc.perform(get("/educators/" + educatorA.getId() + "/students")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenComSubjectOuPapelInvalidoRetorna401() throws Exception {
        String segredo = "segredo-de-teste-com-mais-de-32-caracteres-fixos";
        java.util.Date exp = new java.util.Date(System.currentTimeMillis() + 60_000);
        javax.crypto.SecretKey key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(
            segredo.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        String subjectInvalido = io.jsonwebtoken.Jwts.builder().setSubject("nao-e-uuid").setExpiration(exp)
            .claim("role", "EDUCATOR").signWith(key).compact();
        String papelInvalido = io.jsonwebtoken.Jwts.builder().setSubject(educatorA.getId().toString())
            .setExpiration(exp).claim("role", "ADMIN").signWith(key).compact();

        for (String token : new String[] {subjectInvalido, papelInvalido}) {
            mockMvc.perform(get("/educators/" + educatorA.getId() + "/students")
                    .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void educadorListaProprioAlunos() throws Exception {
        mockMvc.perform(get("/educators/" + educatorA.getId() + "/students")
                .header("Authorization", bearer(educatorA, "EDUCATOR")))
            .andExpect(status().isOk());
    }

    @Test
    void educadorNaoListaAlunosDeOutroEducador() throws Exception {
        mockMvc.perform(get("/educators/" + educatorA.getId() + "/students")
                .header("Authorization", bearer(educatorB, "EDUCATOR")))
            .andExpect(status().isForbidden());
    }

    @Test
    void educadorNaoVinculaAlunoNaContaDeOutroEducador() throws Exception {
        mockMvc.perform(post("/educators/" + educatorA.getId() + "/students")
                .header("Authorization", bearer(educatorB, "EDUCATOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"aluno2@teste.com\",\"studentName\":\"Aluno Dois\"}"))
            .andExpect(status().isForbidden());
        assertTrue(linkRepository.findByEducatorId(educatorA.getId()).stream()
            .noneMatch(l -> l.getStudentId().equals(otherStudent.getId())));
    }

    @Test
    void updateStudentIdsDeOutroEducadorRetorna403() throws Exception {
        mockMvc.perform(put("/educators/" + educatorA.getId() + "/updateStudentIds")
                .header("Authorization", bearer(educatorB, "EDUCATOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json()))
            .andExpect(status().isForbidden());
        assertEquals(1, linkRepository.findByEducatorId(educatorA.getId()).size());
    }

    @Test
    void updateStudentIdsNaoPermiteVincularAlunoArbitrario() throws Exception {
        mockMvc.perform(put("/educators/" + educatorB.getId() + "/updateStudentIds")
                .header("Authorization", bearer(educatorB, "EDUCATOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(otherStudent.getId())))
            .andExpect(status().isBadRequest());
        assertTrue(linkRepository.findByEducatorId(educatorB.getId()).isEmpty());
    }

    @Test
    void updateStudentIdsPermiteRemoverVinculosProprios() throws Exception {
        mockMvc.perform(put("/educators/" + educatorA.getId() + "/updateStudentIds")
                .header("Authorization", bearer(educatorA, "EDUCATOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json()))
            .andExpect(status().isOk());
        assertTrue(linkRepository.findByEducatorId(educatorA.getId()).isEmpty());
    }

    @Test
    void alunoNaoAcessaEndpointsDeEducador() throws Exception {
        mockMvc.perform(put("/educators/" + educatorA.getId() + "/updateStudentIds")
                .header("Authorization", bearer(studentOfA, "STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json()))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/educators/student-progress/" + studentOfA.getId())
                .header("Authorization", bearer(studentOfA, "STUDENT")))
            .andExpect(status().isForbidden());
    }

    @Test
    void educadorVeProgressoSomenteDeAlunoVinculado() throws Exception {
        mockMvc.perform(get("/educators/student-progress/" + studentOfA.getId())
                .header("Authorization", bearer(educatorA, "EDUCATOR")))
            .andExpect(status().isOk());
        mockMvc.perform(get("/educators/student-progress/" + studentOfA.getId())
                .header("Authorization", bearer(educatorB, "EDUCATOR")))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/educators/student-progress/" + otherStudent.getId())
                .header("Authorization", bearer(educatorA, "EDUCATOR")))
            .andExpect(status().isForbidden());
    }

    @Test
    void alunoSoRegistraProgressoDaPropriaConta() throws Exception {
        String body = "{\"currentPhaseIndex\":0,\"errorsData\":1,\"soundRepeatsData\":2}";
        String url = "/players/" + studentOfA.getId() + "/progress";

        mockMvc.perform(post(url)
                .header("Authorization", bearer(otherStudent, "STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isForbidden());

        mockMvc.perform(post(url)
                .header("Authorization", bearer(educatorA, "EDUCATOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isForbidden());

        mockMvc.perform(post(url)
                .header("Authorization", bearer(studentOfA, "STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated());

        mockMvc.perform(post(url)
                .header("Authorization", bearer(studentOfA, "STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isConflict());
    }

    @Test
    void corsPermiteApenasOrigemConfigurada() throws Exception {
        mockMvc.perform(options("/auth/login")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST"))
            .andExpect(status().isOk());
        mockMvc.perform(options("/auth/login")
                .header("Origin", "http://malicioso.example")
                .header("Access-Control-Request-Method", "POST"))
            .andExpect(status().isForbidden());
    }
}
