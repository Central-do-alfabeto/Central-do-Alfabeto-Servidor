package com.centraldoalfabeto.game.domain.controller;

import com.centraldoalfabeto.game.domain.model.Educador;
import com.centraldoalfabeto.game.domain.model.Jogador;
import com.centraldoalfabeto.game.domain.model.TotalAudioReproductions;
import com.centraldoalfabeto.game.domain.model.TotalErrors;
import com.centraldoalfabeto.game.dto.LoginRequestDTO;
import com.centraldoalfabeto.game.dto.UnifiedLoginResponseDTO;
import com.centraldoalfabeto.game.dto.StudentSummaryDTO;
import com.centraldoalfabeto.game.repository.EducadorRepository;
import com.centraldoalfabeto.game.repository.JogadorRepository;
import com.centraldoalfabeto.game.repository.TotalErrorsRepository;
import com.centraldoalfabeto.game.repository.TotalAudioReproductionsRepository;
import com.centraldoalfabeto.game.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
public class AuthController {
    @Autowired
    private JogadorRepository jogadorRepository;

    @Autowired
    private EducadorRepository educadorRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private TotalErrorsRepository totalErrorsRepository;
    
    @Autowired
    private TotalAudioReproductionsRepository totalAudioReproductionsRepository;

    @Autowired
    private JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<UnifiedLoginResponseDTO> login(@RequestBody LoginRequestDTO loginData) {
        if (loginData.getEmail() == null || loginData.getPassword() == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        Boolean requestedStudent = loginData.getIsStudent();

        boolean shouldTryStudent = requestedStudent == null || Boolean.TRUE.equals(requestedStudent);
        boolean shouldTryEducator = requestedStudent == null || Boolean.FALSE.equals(requestedStudent);

        if (shouldTryStudent) {
            Optional<Jogador> optionalJogador = jogadorRepository.findByEmail(loginData.getEmail());

            if (optionalJogador.isPresent() && passwordEncoder.matches(loginData.getPassword(), optionalJogador.get().getSenha())) {
                Jogador foundJogador = optionalJogador.get();

                String token = jwtService.generateToken(foundJogador.getId(), "STUDENT", foundJogador.getEmail());

                UnifiedLoginResponseDTO responseDTO = new UnifiedLoginResponseDTO(
                    foundJogador.getId(),
                    true,
                    foundJogador.getCurrentPhaseIndex(),
                    token
                );
                return new ResponseEntity<>(responseDTO, HttpStatus.OK);
            }
        }

        if (shouldTryEducator) {
            Optional<Educador> optionalEducador = educadorRepository.findByEmail(loginData.getEmail());

            if (optionalEducador.isPresent() && passwordEncoder.matches(loginData.getPassword(), optionalEducador.get().getSenha())) {
                Educador foundEducador = optionalEducador.get();
                
                List<StudentSummaryDTO> studentSummaries = new ArrayList<>();
                if (foundEducador.getStudentIds() != null && !foundEducador.getStudentIds().isEmpty()) {
                    
                    for (Long studentId : foundEducador.getStudentIds()) {
                        Optional<Jogador> optionalJogador = jogadorRepository.findById(studentId);
                        
                        if (optionalJogador.isPresent()) {
                            Jogador jogador = optionalJogador.get();
                            
                            StudentSummaryDTO summary = new StudentSummaryDTO(
                                jogador.getId(), 
                                jogador.getFullName(), 
                                jogador.getCurrentPhaseIndex()
                            );
                            
                            List<TotalErrors> errors = totalErrorsRepository.findErrorsByJogadorId(jogador.getId());
                            Map<Integer, Integer> errorsByPhase = errors.stream()
                                .collect(Collectors.toMap(TotalErrors::getCurrentPhaseIndex, TotalErrors::getValue));
                            summary.setNumberOfErrorsByPhase(errorsByPhase);

                            List<TotalAudioReproductions> soundRepeats = totalAudioReproductionsRepository.findSoundRepeatsByJogadorId(jogador.getId());
                            Map<Integer, Integer> soundRepeatsByPhase = soundRepeats.stream()
                                .collect(Collectors.toMap(TotalAudioReproductions::getCurrentPhaseIndex, TotalAudioReproductions::getValue));
                            summary.setNumberOfSoundRepeatsByPhase(soundRepeatsByPhase);

                            studentSummaries.add(summary);
                        }
                    }
                }
                
                String token = jwtService.generateToken(foundEducador.getId(), "EDUCATOR", foundEducador.getEmail());

                UnifiedLoginResponseDTO responseDTO = new UnifiedLoginResponseDTO(
                    foundEducador.getId(),
                    false,
                    studentSummaries,
                    token
                );
                return new ResponseEntity<>(responseDTO, HttpStatus.OK);
            }
        }

        return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }
}
