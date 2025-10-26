package com.centraldoalfabeto.game.service;

import com.centraldoalfabeto.game.domain.model.User;
import com.centraldoalfabeto.game.domain.model.PlayersData;
import com.centraldoalfabeto.game.domain.model.Jogador;
import com.centraldoalfabeto.game.domain.model.EducatorStudentLink;
import com.centraldoalfabeto.game.dto.AddStudentRequestDTO;
import com.centraldoalfabeto.game.dto.EducatorRegistrationDTO;
import com.centraldoalfabeto.game.dto.StudentProgressDTO;
import com.centraldoalfabeto.game.dto.StudentSummaryDTO;
import com.centraldoalfabeto.game.dto.UnifiedLoginResponseDTO;
import com.centraldoalfabeto.game.repository.UserRepository;
import com.centraldoalfabeto.game.repository.PlayersDataRepository;
import com.centraldoalfabeto.game.repository.JogadorRepository;
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
    private final PlayersDataRepository playersDataRepository;
    private final JogadorRepository jogadorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Autowired
    public EducadorService(
        EducatorStudentLinkRepository educatorStudentLinkRepository,
        UserRepository userRepository,
        PlayersDataRepository playersDataRepository,
        JogadorRepository jogadorRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService
    ) {
        this.educatorStudentLinkRepository = educatorStudentLinkRepository;
        this.userRepository = userRepository;
        this.playersDataRepository = playersDataRepository;
        this.jogadorRepository = jogadorRepository;
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

        if (!jogadorRepository.existsById(studentUser.getId())) {
            throw new IllegalArgumentException("O usuário informado não está cadastrado como aluno.");
        }

        if (educatorStudentLinkRepository.existsByEducatorIdAndStudentId(educatorId, studentUser.getId())) {
            throw new IllegalStateException("Aluno já vinculado a este educador.");
        }

        PlayersData data = playersDataRepository.findById(studentUser.getId())
            .orElseThrow(() -> new IllegalStateException("Dados do aluno não encontrados."));

        educatorStudentLinkRepository.save(new EducatorStudentLink(educatorId, studentUser.getId()));

        return buildStudentSummary(studentUser, data);
    }
    
    public StudentProgressDTO getStudentProgress(UUID educatorId, UUID studentId) throws SecurityException, NoSuchElementException {
        if (!educatorStudentLinkRepository.existsByEducatorIdAndStudentId(educatorId, studentId)) {
            throw new SecurityException("Acesso negado. O aluno não está associado a este educador.");
        }
        
        PlayersData playerData = playersDataRepository.findById(studentId)
            .orElseThrow(() -> new NoSuchElementException("Dados do aluno não encontrados."));
        
        StudentProgressDTO dto = new StudentProgressDTO();
        dto.setCurrentPhaseIndex(playerData.getPhaseIndex());
        dto.setErrorsDataJson(playerData.getErrosTotais());
        dto.setSoundRepeatsDataJson(playerData.getAudiosTotais());
        
        return dto;
    }
    
    @Transactional
    public Set<UUID> updateStudentIds(UUID educatorId, Set<UUID> studentIds) throws IllegalArgumentException {
        getEducatorOrThrow(educatorId);

        if (studentIds != null && !studentIds.isEmpty()) {
            Set<UUID> existingPlayerIds = jogadorRepository.findAllById(studentIds).stream()
                .map(Jogador::getUserId)
                .collect(Collectors.toSet());
            
            if (existingPlayerIds.size() != studentIds.size()) {
                Set<UUID> invalidIds = studentIds.stream()
                    .filter(id -> !existingPlayerIds.contains(id))
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

        Map<UUID, PlayersData> dataById = playersDataRepository.findAllById(studentIds).stream()
            .collect(Collectors.toMap(PlayersData::getPlayersId, Function.identity()));

        List<StudentSummaryDTO> summaries = new ArrayList<>();

        for (UUID studentId : studentIds) {
            PlayersData data = dataById.get(studentId);
            if (data == null) {
                continue;
            }
            User user = usersById.get(studentId);
            summaries.add(buildStudentSummary(user, data));
        }

        return summaries;
    }

    private StudentSummaryDTO buildStudentSummary(User studentUser, PlayersData data) {
        UUID studentId = data.getPlayersId();
        String fullName = studentUser != null ? studentUser.getNome() : null;

        if ((fullName == null || fullName.isBlank()) && data.getPlayer() != null && data.getPlayer().getUser() != null) {
            fullName = data.getPlayer().getUser().getNome();
        }

        if (fullName == null || fullName.isBlank()) {
            fullName = "Aluno";
        }

        Integer phaseIndex = data.getPhaseIndex() != null ? data.getPhaseIndex() : 0;

        StudentSummaryDTO summary = new StudentSummaryDTO(studentId, fullName, phaseIndex);
        summary.setErrorsDataJson(data.getErrosTotais() != null ? data.getErrosTotais() : 0L);
        summary.setSoundRepeatsDataJson(data.getAudiosTotais() != null ? data.getAudiosTotais() : 0L);
        return summary;
    }
}
