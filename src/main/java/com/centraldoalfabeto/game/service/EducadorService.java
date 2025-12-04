package com.centraldoalfabeto.game.service;

import com.centraldoalfabeto.game.domain.model.EducatorStudentLink;
import com.centraldoalfabeto.game.domain.model.PlayerData;
import com.centraldoalfabeto.game.domain.model.User;
import com.centraldoalfabeto.game.dto.AddStudentRequestDTO;
import com.centraldoalfabeto.game.dto.EducatorRegistrationDTO;
import com.centraldoalfabeto.game.dto.PlayerDataSnapshotDTO;
import com.centraldoalfabeto.game.dto.StudentProgressDTO;
import com.centraldoalfabeto.game.dto.StudentSummaryDTO;
import com.centraldoalfabeto.game.dto.UnifiedLoginResponseDTO;
import com.centraldoalfabeto.game.repository.UserRepository;
import com.centraldoalfabeto.game.repository.PlayerDataRepository;
import com.centraldoalfabeto.game.repository.EducatorStudentLinkRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class EducadorService {
    private final EducatorStudentLinkRepository educatorStudentLinkRepository;
    private final UserRepository userRepository;
    private final PlayerDataRepository playerDataRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Autowired
    public EducadorService(
        EducatorStudentLinkRepository educatorStudentLinkRepository,
        UserRepository userRepository,
        PlayerDataRepository playerDataRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService
    ) {
        this.educatorStudentLinkRepository = educatorStudentLinkRepository;
        this.userRepository = userRepository;
        this.playerDataRepository = playerDataRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public UnifiedLoginResponseDTO registerEducator(EducatorRegistrationDTO dto) throws IllegalArgumentException {
        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email já registrado.");
        }
        
        User user = new User();
        user.setNome(dto.getNome());
        user.setEmail(dto.getEmail());
        user.setSenhaHash(passwordEncoder.encode(dto.getSenha()));
        user.setMetadados("{\"role\":\"educator\"}");
        user = userRepository.save(user);

        String token = jwtService.generateToken(user.getId(), "EDUCATOR", user.getEmail());
        
        UnifiedLoginResponseDTO response = new UnifiedLoginResponseDTO(
            user.getId(), 
            false,
            (Integer) null,
            token
        );
        response.setRole("EDUCATOR");
        response.setUserName(user.getNome());
        response.setEmail(user.getEmail());
        return response;
    }

    public List<StudentSummaryDTO> listStudents(UUID educatorId) {
        getEducatorOrThrow(educatorId);

        Set<UUID> studentIds = educatorStudentLinkRepository.findByEducatorId(educatorId).stream()
            .map(EducatorStudentLink::getStudentId)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        return buildStudentSummaries(studentIds);
    }

    @Transactional
    public StudentSummaryDTO addStudent(UUID educatorId, AddStudentRequestDTO request) {
        getEducatorOrThrow(educatorId);

        String email = request.getEmail() != null ? request.getEmail().trim() : "";
        String studentName = request.getStudentName() != null ? request.getStudentName().trim() : "";

        if (email.isEmpty()) {
            throw new IllegalArgumentException("Email do aluno é obrigatório.");
        }

        if (studentName.isEmpty()) {
            throw new IllegalArgumentException("Nome do aluno é obrigatório.");
        }

        User studentUser = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("Aluno não encontrado com o email informado."));

        String storedStudentName = studentUser.getNome() != null ? studentUser.getNome().trim() : "";
        if (!storedStudentName.equalsIgnoreCase(studentName)) {
            throw new IllegalArgumentException("Nome do aluno não confere com o cadastro.");
        }

        if (!isStudentUser(studentUser)) {
            throw new IllegalArgumentException("O usuário informado não está cadastrado como aluno.");
        }

        if (educatorStudentLinkRepository.existsByEducatorIdAndStudentId(educatorId, studentUser.getId())) {
            throw new IllegalStateException("Aluno já vinculado a este educador.");
        }

        educatorStudentLinkRepository.save(new EducatorStudentLink(educatorId, studentUser.getId()));
        return buildStudentSummary(studentUser);
    }
    
    public StudentProgressDTO getStudentProgress(UUID educatorId, UUID studentId) throws SecurityException, NoSuchElementException {
        if (!educatorStudentLinkRepository.existsByEducatorIdAndStudentId(educatorId, studentId)) {
            throw new SecurityException("Acesso negado. O aluno não está associado a este educador.");
        }
        
        User studentUser = userRepository.findById(studentId)
            .orElseThrow(() -> new NoSuchElementException("Aluno não encontrado."));

        List<PlayerData> playerSnapshots = playerDataRepository.findByPlayerIdOrderByPhaseIndexAsc(studentId);

        List<PlayerDataSnapshotDTO> snapshotDTOs = playerSnapshots.stream()
            .map(snapshot -> new PlayerDataSnapshotDTO(
                snapshot.getPhaseIndex(),
                snapshot.getErrosTotais() != null ? snapshot.getErrosTotais() : 0L,
                snapshot.getReproducoesTotais() != null ? snapshot.getReproducoesTotais() : 0L
            ))
            .collect(Collectors.toList());

        long totalErrors = snapshotDTOs.stream()
            .mapToLong(PlayerDataSnapshotDTO::getTotalErrors)
            .sum();

        long totalRepeats = snapshotDTOs.stream()
            .mapToLong(PlayerDataSnapshotDTO::getTotalAudioReproductions)
            .sum();

        Integer lastCompletedPhase = snapshotDTOs.isEmpty()
            ? null
            : snapshotDTOs.get(snapshotDTOs.size() - 1).getPhaseIndex();

        StudentProgressDTO dto = new StudentProgressDTO();
        dto.setStudentId(studentId);
        dto.setStudentName(studentUser.getNome());
        dto.setStudentEmail(studentUser.getEmail());
        dto.setCurrentPhaseIndex(resolveNextPhaseIndex(studentId));
        dto.setLastCompletedPhaseIndex(lastCompletedPhase);
        dto.setErrorsDataJson(totalErrors);
        dto.setSoundRepeatsDataJson(totalRepeats);
        dto.setSnapshots(snapshotDTOs);

        return dto;
    }
    
    @Transactional
    public Set<UUID> updateStudentIds(UUID educatorId, Set<UUID> studentIds) throws IllegalArgumentException {
        getEducatorOrThrow(educatorId);

        if (studentIds != null && !studentIds.isEmpty()) {
            Set<UUID> validIds = userRepository.findAllById(studentIds).stream()
                .filter(this::isStudentUser)
                .map(User::getId)
                .collect(Collectors.toSet());

            if (validIds.size() != studentIds.size()) {
                Set<UUID> invalidIds = studentIds.stream()
                    .filter(id -> !validIds.contains(id))
                    .collect(Collectors.toSet());

                throw new IllegalArgumentException("Um ou mais IDs de aluno são inválidos ou não encontrados: " + invalidIds);
            }
        }
        
        educatorStudentLinkRepository.deleteByEducatorId(educatorId);

        if (studentIds != null && !studentIds.isEmpty()) {
            for (UUID studentId : studentIds) {
                educatorStudentLinkRepository.save(new EducatorStudentLink(educatorId, studentId));
            }
        }

        return studentIds != null ? Set.copyOf(studentIds) : Set.of();
    }

    private User getEducatorOrThrow(UUID educatorId) {
        return userRepository.findById(educatorId)
            .filter(user -> {
                String metadata = user.getMetadados();
                return metadata != null && metadata.toLowerCase().contains("educator");
            })
            .orElseThrow(() -> new IllegalArgumentException("Educador não encontrado."));
    }

    private List<StudentSummaryDTO> buildStudentSummaries(Set<UUID> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return List.of();
        }

        Map<UUID, User> usersById = userRepository.findAllById(studentIds).stream()
            .collect(Collectors.toMap(User::getId, Function.identity()));
        List<StudentSummaryDTO> summaries = new ArrayList<>();

        for (UUID studentId : studentIds) {
            User user = usersById.get(studentId);
            summaries.add(buildStudentSummary(user));
        }

        return summaries;
    }

    private StudentSummaryDTO buildStudentSummary(User studentUser) {
        if (studentUser == null) {
            throw new IllegalArgumentException("Aluno não encontrado.");
        }

        String fullName = studentUser.getNome();
        if (fullName == null || fullName.isBlank()) {
            fullName = "Aluno";
        }

        int nextPhase = resolveNextPhaseIndex(studentUser.getId());

        StudentSummaryDTO summary = new StudentSummaryDTO(studentUser.getId(), fullName, nextPhase);
        summary.setErrorsDataJson(0L);
        summary.setSoundRepeatsDataJson(0L);
        return summary;
    }

    private boolean isStudentUser(User user) {
        if (user == null) {
            return false;
        }

        String metadata = user.getMetadados();
        if (metadata == null) {
            return playerDataRepository.existsByPlayerId(user.getId());
        }

        String normalized = metadata.toLowerCase();
        if (normalized.contains("student") || normalized.contains("player") || normalized.contains("aluno")) {
            return true;
        }

        return playerDataRepository.existsByPlayerId(user.getId());
    }

    private int resolveNextPhaseIndex(UUID playerId) {
        return playerDataRepository.findLatestPhaseIndexByPlayerId(playerId)
            .map(latest -> latest + 1)
            .orElse(0);
    }
}
