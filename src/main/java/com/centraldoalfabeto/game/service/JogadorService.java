package com.centraldoalfabeto.game.service;

import com.centraldoalfabeto.game.domain.model.Jogador;
import com.centraldoalfabeto.game.domain.model.PlayersData;
import com.centraldoalfabeto.game.domain.model.User;
import com.centraldoalfabeto.game.dto.PlayerRegistrationDTO;
import com.centraldoalfabeto.game.dto.UnifiedLoginResponseDTO;
import com.centraldoalfabeto.game.repository.JogadorRepository;
import com.centraldoalfabeto.game.repository.PlayersDataRepository;
import com.centraldoalfabeto.game.repository.UserRepository;
import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class JogadorService {
    private final JogadorRepository jogadorRepository;
    private final UserRepository userRepository;
    private final PlayersDataRepository playersDataRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Autowired
    public JogadorService(
        JogadorRepository jogadorRepository,
        UserRepository userRepository,
        PlayersDataRepository playersDataRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService
    ) {
        this.jogadorRepository = jogadorRepository;
        this.userRepository = userRepository;
        this.playersDataRepository = playersDataRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public UnifiedLoginResponseDTO registerPlayer(PlayerRegistrationDTO dto) throws IllegalArgumentException {
        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email já registrado.");
        }
        
        User user = new User();
        user.setNome(dto.getNome());
        user.setEmail(dto.getEmail());
        user.setSenhaHash(passwordEncoder.encode(dto.getSenha()));
        user.setMetadados("{\"role\":\"aluno\"}");
        user = userRepository.save(user);

        Jogador jogador = new Jogador();
        jogador.setUser(user);
        jogador.setCurrentPhaseIndex(0);
        jogador = jogadorRepository.save(jogador);

        PlayersData initialData = new PlayersData();
        initialData.setPlayer(jogador);
        initialData.setPhaseIndex(0);
    initialData.setAudiosTotais(0L);
    initialData.setErrosTotais(0L);
        playersDataRepository.save(initialData);

        String token = jwtService.generateToken(user.getId(), "STUDENT", user.getEmail());
        
        return new UnifiedLoginResponseDTO(
            user.getId(), 
            true,
            initialData.getPhaseIndex(),
            token
        );
    }
}
