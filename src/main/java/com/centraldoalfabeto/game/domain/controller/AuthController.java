package com.centraldoalfabeto.game.domain.controller;

import com.centraldoalfabeto.game.domain.model.Educador;
import com.centraldoalfabeto.game.domain.model.Jogador;
import com.centraldoalfabeto.game.dto.LoginRequestDTO;
import com.centraldoalfabeto.game.dto.UnifiedLoginResponseDTO;
import com.centraldoalfabeto.game.repository.EducadorRepository;
import com.centraldoalfabeto.game.repository.JogadorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {
    @Autowired
    private JogadorRepository jogadorRepository;

    @Autowired
    private EducadorRepository educadorRepository;

    @PostMapping("/login")
    public ResponseEntity<UnifiedLoginResponseDTO> login(@RequestBody LoginRequestDTO loginData) {
        if (loginData.getEmail() == null || loginData.getPassword() == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        if (loginData.getIsStudent()) {
            Optional<Jogador> optionalJogador = jogadorRepository.findByEmail(loginData.getEmail());

            if (optionalJogador.isPresent() && optionalJogador.get().getPassword().equals(loginData.getPassword())) {
                Jogador foundJogador = optionalJogador.get();

                UnifiedLoginResponseDTO responseDTO = new UnifiedLoginResponseDTO(foundJogador.getId(), true, foundJogador.getCurrentPhaseIndex());
                return new ResponseEntity<>(responseDTO, HttpStatus.OK);
            }
        } else {
            Optional<Educador> optionalEducador = educadorRepository.findByEmail(loginData.getEmail());

            if (optionalEducador.isPresent() && optionalEducador.get().getPassword().equals(loginData.getPassword())) {
                Educador foundEducador = optionalEducador.get();

                Map<Long, String> students = new HashMap<>();
                if (foundEducador.getStudentIds() != null && !foundEducador.getStudentIds().isEmpty()) {
                    for (Long studentId : foundEducador.getStudentIds()) {
                        Optional<Jogador> optionalJogador = jogadorRepository.findById(studentId);
                        optionalJogador.ifPresent(jogador -> students.put(jogador.getId(), jogador.getFullName()));
                    }
                }
                
                UnifiedLoginResponseDTO responseDTO = new UnifiedLoginResponseDTO(foundEducador.getId(), false, students);
                return new ResponseEntity<>(responseDTO, HttpStatus.OK);
            }
        }

        return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }
}
