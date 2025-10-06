package com.centraldoalfabeto.game.service;

import com.centraldoalfabeto.game.domain.model.Educador;
import com.centraldoalfabeto.game.domain.model.Jogador;
import com.centraldoalfabeto.game.domain.model.User;
import com.centraldoalfabeto.game.domain.model.PlayersData;
import com.centraldoalfabeto.game.dto.LoginRequestDTO;
import com.centraldoalfabeto.game.dto.UnifiedLoginResponseDTO;
import com.centraldoalfabeto.game.dto.StudentSummaryDTO;
import com.centraldoalfabeto.game.repository.UserRepository;
import com.centraldoalfabeto.game.repository.EducadorRepository;
import com.centraldoalfabeto.game.repository.JogadorRepository;
import com.centraldoalfabeto.game.repository.PlayersDataRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final JogadorRepository jogadorRepository;
    private final EducadorRepository educadorRepository;
    private final PlayersDataRepository playersDataRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Autowired
    public AuthService(
        UserRepository userRepository,
        JogadorRepository jogadorRepository,
        EducadorRepository educadorRepository,
        PlayersDataRepository playersDataRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.jogadorRepository = jogadorRepository;
        this.educadorRepository = educadorRepository;
        this.playersDataRepository = playersDataRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public UnifiedLoginResponseDTO authenticate(LoginRequestDTO loginData) throws SecurityException {
        User user = userRepository.findByEmail(loginData.getEmail())
            .orElseThrow(() -> new SecurityException("Credenciais inválidas."));
            
        if (!passwordEncoder.matches(loginData.getPassword(), user.getSenhaHash())) {
            throw new SecurityException("Credenciais inválidas.");
        }

        UUID userId = user.getId();
        boolean isStudent = loginData.getIsStudent() == null || Boolean.TRUE.equals(loginData.getIsStudent());

        if (isStudent && jogadorRepository.existsById(userId)) {
            return buildPlayerResponse(user);
        }
        
        if (!isStudent && educadorRepository.existsById(userId)) {
            return buildEducatorResponse(user);
        }

        throw new SecurityException("Papel solicitado incorreto para o usuário.");
    }
    
    private UnifiedLoginResponseDTO buildPlayerResponse(User user) {
        Optional<PlayersData> optionalData = playersDataRepository.findById(user.getId());
        
        PlayersData playerData = optionalData.orElseGet(() -> {
            PlayersData data = new PlayersData();
            data.setPhaseIndex(0);
            return data;
        });

        String token = jwtService.generateToken(user.getId(), "STUDENT", user.getEmail());
        
        return new UnifiedLoginResponseDTO(
            user.getId(),
            true,
            playerData.getPhaseIndex(),
            token
        );
    }

    private UnifiedLoginResponseDTO buildEducatorResponse(User user) {
        Educador educator = educadorRepository.findById(user.getId())
            .orElseThrow(() -> new IllegalStateException("Educador não encontrado após autenticação."));

        List<StudentSummaryDTO> studentSummaries = List.of();
        
        Set<UUID> studentIds = educator.getStudentIds();
        
        if (studentIds != null && !studentIds.isEmpty()) {
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

        return new UnifiedLoginResponseDTO(
            user.getId(),
            false,
            studentSummaries,
            token
        );
    }
}
