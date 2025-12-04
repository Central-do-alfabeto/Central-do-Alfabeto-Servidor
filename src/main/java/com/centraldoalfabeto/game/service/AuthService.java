package com.centraldoalfabeto.game.service;

import com.centraldoalfabeto.game.domain.model.User;
import com.centraldoalfabeto.game.domain.model.EducatorStudentLink;
import com.centraldoalfabeto.game.dto.LoginRequestDTO;
import com.centraldoalfabeto.game.dto.UnifiedLoginResponseDTO;
import com.centraldoalfabeto.game.dto.StudentSummaryDTO;
import com.centraldoalfabeto.game.repository.UserRepository;
import com.centraldoalfabeto.game.repository.PlayerDataRepository;
import com.centraldoalfabeto.game.repository.EducatorStudentLinkRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository userRepository;
    private final EducatorStudentLinkRepository educatorStudentLinkRepository;
    private final PlayerDataRepository playerDataRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    @Autowired
    public AuthService(
        UserRepository userRepository,
        EducatorStudentLinkRepository educatorStudentLinkRepository,
        PlayerDataRepository playerDataRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        ObjectMapper objectMapper
    ) {
        this.userRepository = userRepository;
        this.educatorStudentLinkRepository = educatorStudentLinkRepository;
        this.playerDataRepository = playerDataRepository;
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

        if (isStudent) {
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
        int nextPhaseIndex = resolveNextPhaseIndex(user.getId());

        String token = jwtService.generateToken(user.getId(), "STUDENT", user.getEmail());

        UnifiedLoginResponseDTO dto = new UnifiedLoginResponseDTO(
            user.getId(),
            true,
            nextPhaseIndex,
            token
        );
        dto.setRole("STUDENT");
        dto.setUserName(user.getNome());
        dto.setEmail(user.getEmail());
        return dto;
    }

    private UnifiedLoginResponseDTO buildEducatorResponse(User user) {
        List<EducatorStudentLink> links = educatorStudentLinkRepository.findByEducatorId(user.getId());

        Set<UUID> studentIds = links.stream()
            .map(EducatorStudentLink::getStudentId)
            .collect(Collectors.toSet());

        List<StudentSummaryDTO> studentSummaries = List.of();

        if (!studentIds.isEmpty()) {
            Map<UUID, User> usersById = userRepository.findAllById(studentIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

            studentSummaries = studentIds.stream()
                .map(studentId -> {
                    User studentUser = usersById.get(studentId);
                    String fullName = studentUser != null && studentUser.getNome() != null
                        ? studentUser.getNome()
                        : "Aluno Desconhecido";

                    int currentPhase = resolveNextPhaseIndex(studentId);

                    StudentSummaryDTO summary = new StudentSummaryDTO(
                        studentId,
                        fullName,
                        currentPhase
                    );

                    summary.setErrorsDataJson(0L);
                    summary.setSoundRepeatsDataJson(0L);
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
        dto.setUserName(user.getNome());
        dto.setEmail(user.getEmail());
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

        if (playerDataRepository.existsByPlayerId(userId)) {
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

    private int resolveNextPhaseIndex(UUID playerId) {
        return playerDataRepository.findLatestPhaseIndexByPlayerId(playerId)
            .map(latest -> latest + 1)
            .orElse(0);
    }
}
