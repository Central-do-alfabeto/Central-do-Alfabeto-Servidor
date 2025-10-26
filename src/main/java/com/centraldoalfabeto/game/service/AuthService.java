package com.centraldoalfabeto.game.service;

import com.centraldoalfabeto.game.domain.model.Jogador;
import com.centraldoalfabeto.game.domain.model.User;
import com.centraldoalfabeto.game.domain.model.PlayersData;
import com.centraldoalfabeto.game.domain.model.EducatorStudentLink;
import com.centraldoalfabeto.game.dto.LoginRequestDTO;
import com.centraldoalfabeto.game.dto.UnifiedLoginResponseDTO;
import com.centraldoalfabeto.game.dto.StudentSummaryDTO;
import com.centraldoalfabeto.game.repository.UserRepository;
import com.centraldoalfabeto.game.repository.JogadorRepository;
import com.centraldoalfabeto.game.repository.PlayersDataRepository;
import com.centraldoalfabeto.game.repository.EducatorStudentLinkRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository userRepository;
    private final JogadorRepository jogadorRepository;
    private final EducatorStudentLinkRepository educatorStudentLinkRepository;
    private final PlayersDataRepository playersDataRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    @Autowired
    public AuthService(
        UserRepository userRepository,
        JogadorRepository jogadorRepository,
        EducatorStudentLinkRepository educatorStudentLinkRepository,
        PlayersDataRepository playersDataRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        ObjectMapper objectMapper
    ) {
        this.userRepository = userRepository;
        this.jogadorRepository = jogadorRepository;
        this.educatorStudentLinkRepository = educatorStudentLinkRepository;
        this.playersDataRepository = playersDataRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
    }

    public UnifiedLoginResponseDTO authenticate(LoginRequestDTO loginData) throws SecurityException {
        User user = userRepository.findByEmail(loginData.getEmail())
            .orElseThrow(() -> new SecurityException("Credenciais inválidas."));
            
        if (!passwordEncoder.matches(loginData.getPassword(), user.getSenhaHash())) {
            throw new SecurityException("Credenciais inválidas.");
        }

        UUID userId = user.getId();
        String metadataRole = resolveRoleFromMetadata(user.getMetadados());
        Boolean requestedStudent = loginData.getIsStudent();
        boolean isStudent = determineIsStudent(metadataRole, requestedStudent, userId);

        UnifiedLoginResponseDTO response;

        if (isStudent && jogadorRepository.existsById(userId)) {
            response = buildPlayerResponse(user);
        } else if (!isStudent) {
            response = buildEducatorResponse(user);
        }

        else {
            throw new SecurityException("Papel solicitado incorreto para o usuário.");
        }

        response.setRole(metadataRole != null ? metadataRole : (isStudent ? "STUDENT" : "EDUCATOR"));
        return response;
    }
    
    private UnifiedLoginResponseDTO buildPlayerResponse(User user) {
        Optional<PlayersData> optionalData = playersDataRepository.findById(user.getId());
        
        PlayersData playerData = optionalData.orElseGet(() -> {
            PlayersData data = new PlayersData();
            data.setPhaseIndex(0);
            return data;
        });

        String token = jwtService.generateToken(user.getId(), "STUDENT", user.getEmail());
        
        UnifiedLoginResponseDTO dto = new UnifiedLoginResponseDTO(
            user.getId(),
            true,
            playerData.getPhaseIndex(),
            token
        );
        dto.setRole("STUDENT");
        return dto;
    }

    private UnifiedLoginResponseDTO buildEducatorResponse(User user) {
        List<EducatorStudentLink> links = educatorStudentLinkRepository.findByEducatorId(user.getId());

        Set<UUID> studentIds = links.stream()
            .map(EducatorStudentLink::getStudentId)
            .collect(Collectors.toSet());

        List<StudentSummaryDTO> studentSummaries = List.of();

        if (!studentIds.isEmpty()) {
            List<PlayersData> allStudentsData = playersDataRepository.findAllById(studentIds);

            studentSummaries = allStudentsData.stream()
                .map(data -> {
                    Optional<Jogador> optionalJogador = jogadorRepository.findById(data.getPlayersId());
                    String fullName = optionalJogador.map(Jogador::getUser).map(User::getNome).orElse("Aluno Desconhecido");

                    StudentSummaryDTO summary = new StudentSummaryDTO(
                        data.getPlayersId(),
                        fullName,
                        data.getPhaseIndex()
                    );

                    summary.setErrorsDataJson(data.getErrosTotais());
                    summary.setSoundRepeatsDataJson(data.getAudiosTotais());
                    return summary;
                })
                .collect(Collectors.toList());
        }

        String token = jwtService.generateToken(user.getId(), "EDUCATOR", user.getEmail());

        UnifiedLoginResponseDTO dto = new UnifiedLoginResponseDTO(
            user.getId(),
            false,
            studentSummaries,
            token
        );
        dto.setRole("EDUCATOR");
        return dto;
    }

    private boolean determineIsStudent(String metadataRole, Boolean requestedStudent, UUID userId) {
        if (metadataRole != null) {
            if (metadataRole.equalsIgnoreCase("aluno")
                || metadataRole.equalsIgnoreCase("student")
                || metadataRole.equalsIgnoreCase("player")) {
                return true;
            }
            if (metadataRole.equalsIgnoreCase("educador")
                || metadataRole.equalsIgnoreCase("educator")) {
                return false;
            }
        }

        if (requestedStudent != null) {
            return requestedStudent;
        }

        // fallback to database relationships if metadata missing
        if (jogadorRepository.existsById(userId)) {
            return true;
        }

        if (educatorStudentLinkRepository.existsByEducatorId(userId)) {
            return false;
        }

        // default to student if data inconclusive
        return true;
    }

    private String resolveRoleFromMetadata(String rawMetadata) {
        if (rawMetadata == null || rawMetadata.isBlank()) {
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(rawMetadata);
            JsonNode roleNode = root.path("role");
            if (roleNode.isTextual()) {
                String roleValue = roleNode.asText();
                if (roleValue != null && !roleValue.isBlank()) {
                    return roleValue;
                }
            }
        } catch (Exception ex) {
            logger.warn("Não foi possível interpretar metadados do usuário:", ex);
        }

        return null;
    }
}
