package com.centraldoalfabeto.game.service;

import com.centraldoalfabeto.game.domain.model.Educador;
import com.centraldoalfabeto.game.domain.model.User;
import com.centraldoalfabeto.game.domain.model.PlayersData;
import com.centraldoalfabeto.game.domain.model.Jogador;
import com.centraldoalfabeto.game.dto.EducatorRegistrationDTO;
import com.centraldoalfabeto.game.dto.StudentProgressDTO;
import com.centraldoalfabeto.game.dto.UnifiedLoginResponseDTO;
import com.centraldoalfabeto.game.repository.EducadorRepository;
import com.centraldoalfabeto.game.repository.UserRepository;
import com.centraldoalfabeto.game.repository.PlayersDataRepository;
import com.centraldoalfabeto.game.repository.JogadorRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
public class EducadorService {
    private final EducadorRepository educadorRepository;
    private final UserRepository userRepository;
    private final PlayersDataRepository playersDataRepository;
    private final JogadorRepository jogadorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Autowired
    public EducadorService(
        EducadorRepository educadorRepository,
        UserRepository userRepository,
        PlayersDataRepository playersDataRepository,
        JogadorRepository jogadorRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService
    ) {
        this.educadorRepository = educadorRepository;
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
        user = userRepository.save(user);
        
        Educador educator = new Educador();
        educator.setUserId(user.getId()); 
        educator.setUser(user);
        
        educator = educadorRepository.save(educator);

        String token = jwtService.generateToken(user.getId(), "EDUCATOR", user.getEmail());
        
        return new UnifiedLoginResponseDTO(
            user.getId(), 
            false,
            (Integer) null,
            token
        );
    }
    
    public StudentProgressDTO getStudentProgress(UUID educatorId, UUID studentId) throws SecurityException, NoSuchElementException {
        Educador educator = educadorRepository.findById(educatorId)
            .orElseThrow(() -> new NoSuchElementException("Educador não encontrado."));
        
        Set<UUID> allowedStudents = educator.getStudentIds();
        
        if (allowedStudents == null || !allowedStudents.contains(studentId)) {
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
    public Educador updateStudentIds(UUID educatorId, Set<UUID> studentIds) throws NoSuchElementException, IllegalArgumentException {
        Educador educator = educadorRepository.findById(educatorId)
            .orElseThrow(() -> new NoSuchElementException("Educador não encontrado."));
        
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
        
        educator.setStudentIds(studentIds); 
        
        return educadorRepository.save(educator);
    }
}
