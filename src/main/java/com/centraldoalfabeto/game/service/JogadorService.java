package com.centraldoalfabeto.game.service;

import com.centraldoalfabeto.game.domain.model.User;
import com.centraldoalfabeto.game.dto.PlayerRegistrationDTO;
import com.centraldoalfabeto.game.dto.UnifiedLoginResponseDTO;
import com.centraldoalfabeto.game.repository.UserRepository;
import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class JogadorService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Autowired
    public JogadorService(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService
    ) {
        this.userRepository = userRepository;
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
        user.setMetadados("{\"role\":\"student\"}");
        user = userRepository.save(user);

        String token = jwtService.generateToken(user.getId(), "STUDENT", user.getEmail());

        UnifiedLoginResponseDTO response = new UnifiedLoginResponseDTO(
            user.getId(),
            true,
            0,
            token
        );
        response.setRole("STUDENT");
        response.setUserName(user.getNome());
        response.setEmail(user.getEmail());
        return response;
    }
}
