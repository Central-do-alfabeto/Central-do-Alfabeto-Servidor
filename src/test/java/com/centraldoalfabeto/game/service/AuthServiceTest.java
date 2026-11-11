package com.centraldoalfabeto.game.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.centraldoalfabeto.game.domain.model.User;
import com.centraldoalfabeto.game.dto.EducatorRegistrationDTO;
import com.centraldoalfabeto.game.dto.LoginRequestDTO;
import com.centraldoalfabeto.game.dto.PlayerRegistrationDTO;
import com.centraldoalfabeto.game.dto.UnifiedLoginResponseDTO;
import com.centraldoalfabeto.game.repository.EducatorStudentLinkRepository;
import com.centraldoalfabeto.game.repository.PlayerDataRepository;
import com.centraldoalfabeto.game.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {

    private static final String SECRET = "segredo-de-teste-com-mais-de-32-caracteres-fixos";

    private UserRepository userRepository;
    private EducatorStudentLinkRepository linkRepository;
    private PlayerDataRepository playerDataRepository;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private AuthService authService;
    private JogadorService jogadorService;
    private EducadorService educadorService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        linkRepository = mock(EducatorStudentLinkRepository.class);
        playerDataRepository = mock(PlayerDataRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        jwtService = new JwtService(SECRET, 3_600_000L);
        jwtService.init();
        authService = new AuthService(userRepository, linkRepository, playerDataRepository,
            passwordEncoder, jwtService, new ObjectMapper());
        jogadorService = new JogadorService(userRepository, passwordEncoder, jwtService);
        educadorService = new EducadorService(linkRepository, userRepository, playerDataRepository,
            passwordEncoder, jwtService);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            if (u.getId() == null) {
                u.setId(UUID.randomUUID());
            }
            return u;
        });
    }

    private User existingUser(String email, String rawPassword, String role) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setNome("Fulano");
        user.setEmail(email);
        user.setSenhaHash(passwordEncoder.encode(rawPassword));
        user.setMetadados("{\"role\":\"" + role + "\"}");
        return user;
    }

    private LoginRequestDTO login(String email, String password) {
        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setEmail(email);
        dto.setPassword(password);
        return dto;
    }

    @Test
    void cadastroDeAlunoGravaHashBcryptEGeraToken() {
        when(userRepository.findByEmail("aluno@teste.com")).thenReturn(Optional.empty());
        PlayerRegistrationDTO dto = new PlayerRegistrationDTO();
        dto.setNome("Aluno");
        dto.setEmail("aluno@teste.com");
        dto.setSenha("senha-secreta");

        UnifiedLoginResponseDTO response = jogadorService.registerPlayer(dto);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertNotEquals("senha-secreta", saved.getSenhaHash());
        assertTrue(saved.getSenhaHash().startsWith("$2"), "esperado hash BCrypt");
        assertTrue(passwordEncoder.matches("senha-secreta", saved.getSenhaHash()));
        assertTrue(saved.getMetadados().contains("student"));

        assertTrue(response.isStudent());
        assertEquals("STUDENT", response.getRole());
        assertEquals(0, response.getCurrentPhaseIndex());
        Claims claims = jwtService.extractClaims(response.getToken());
        assertEquals(saved.getId().toString(), claims.getSubject());
        assertEquals("STUDENT", claims.get("role", String.class));
    }

    @Test
    void cadastroDeEducadorGravaHashBcryptEGeraToken() {
        when(userRepository.findByEmail("edu@teste.com")).thenReturn(Optional.empty());
        EducatorRegistrationDTO dto = new EducatorRegistrationDTO();
        dto.setNome("Educadora");
        dto.setEmail("edu@teste.com");
        dto.setSenha("outra-senha");

        UnifiedLoginResponseDTO response = educadorService.registerEducator(dto);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertTrue(passwordEncoder.matches("outra-senha", captor.getValue().getSenhaHash()));
        assertFalse(response.isStudent());
        assertEquals("EDUCATOR", response.getRole());
        assertEquals("EDUCATOR", jwtService.extractClaims(response.getToken()).get("role", String.class));
    }

    @Test
    void cadastroComEmailDuplicadoERejeitado() {
        when(userRepository.findByEmail("dup@teste.com"))
            .thenReturn(Optional.of(existingUser("dup@teste.com", "x", "student")));
        PlayerRegistrationDTO dto = new PlayerRegistrationDTO();
        dto.setNome("Aluno");
        dto.setEmail("dup@teste.com");
        dto.setSenha("senha");

        assertThrows(IllegalArgumentException.class, () -> jogadorService.registerPlayer(dto));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void loginDeAlunoRetornaTokenComPapelEProximaFase() {
        User user = existingUser("aluno@teste.com", "senha123", "student");
        when(userRepository.findByEmail("aluno@teste.com")).thenReturn(Optional.of(user));
        when(playerDataRepository.findLatestPhaseIndexByPlayerId(user.getId())).thenReturn(Optional.of(4));

        UnifiedLoginResponseDTO response = authService.authenticate(login("aluno@teste.com", "senha123"));

        assertTrue(response.isStudent());
        assertEquals("STUDENT", response.getRole());
        assertEquals(5, response.getCurrentPhaseIndex());
        assertTrue(jwtService.isTokenValid(response.getToken()));
        assertEquals(user.getId().toString(), jwtService.extractClaims(response.getToken()).getSubject());
    }

    @Test
    void loginDeEducadorRetornaTokenDeEducador() {
        User user = existingUser("edu@teste.com", "senha123", "educator");
        when(userRepository.findByEmail("edu@teste.com")).thenReturn(Optional.of(user));
        when(linkRepository.findByEducatorId(user.getId())).thenReturn(List.of());

        UnifiedLoginResponseDTO response = authService.authenticate(login("edu@teste.com", "senha123"));

        assertFalse(response.isStudent());
        assertEquals("EDUCATOR", response.getRole());
        assertEquals("EDUCATOR", jwtService.extractClaims(response.getToken()).get("role", String.class));
    }

    @Test
    void loginComSenhaIncorretaFalha() {
        User user = existingUser("aluno@teste.com", "senha123", "student");
        when(userRepository.findByEmail("aluno@teste.com")).thenReturn(Optional.of(user));

        assertThrows(SecurityException.class,
            () -> authService.authenticate(login("aluno@teste.com", "errada")));
    }

    @Test
    void loginComEmailInexistenteFalha() {
        when(userRepository.findByEmail("nao@existe.com")).thenReturn(Optional.empty());

        assertThrows(SecurityException.class,
            () -> authService.authenticate(login("nao@existe.com", "qualquer")));
    }

    @Test
    void jwtServiceRejeitaTokenAdulteradoEExpirado() {
        String token = jwtService.generateToken(UUID.randomUUID(), "student", "a@b.com");
        assertTrue(jwtService.isTokenValid(token));
        assertFalse(jwtService.isTokenValid(token + "x"));

        JwtService expirado = new JwtService(SECRET, -1000L);
        expirado.init();
        assertFalse(jwtService.isTokenValid(expirado.generateToken(UUID.randomUUID(), "student", "a@b.com")));
    }

    @Test
    void jwtServiceExigeSegredoValido() {
        JwtService semSegredo = new JwtService("", 1000L);
        IllegalStateException vazio = assertThrows(IllegalStateException.class, semSegredo::init);
        assertTrue(vazio.getMessage().contains("JWT_SECRET"));

        JwtService curto = new JwtService("curto", 1000L);
        IllegalStateException erro = assertThrows(IllegalStateException.class, curto::init);
        assertTrue(erro.getMessage().contains("JWT_SECRET"));
    }

    @Test
    void loginRecusaContaSemPapelNosMetadados() {
        User semPapel = existingUser("sem@teste.com", "senha123", "student");
        semPapel.setMetadados(null);
        when(userRepository.findByEmail("sem@teste.com")).thenReturn(Optional.of(semPapel));

        assertThrows(SecurityException.class,
            () -> authService.authenticate(login("sem@teste.com", "senha123")));
    }

    @Test
    void loginRecusaPapelDesconhecido() {
        User estranho = existingUser("estranho@teste.com", "senha123", "admin");
        when(userRepository.findByEmail("estranho@teste.com")).thenReturn(Optional.of(estranho));

        assertThrows(SecurityException.class,
            () -> authService.authenticate(login("estranho@teste.com", "senha123")));
    }
}
