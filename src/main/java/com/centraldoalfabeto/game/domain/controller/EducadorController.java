package com.centraldoalfabeto.game.domain.controller;

import com.centraldoalfabeto.game.domain.model.Educador;
import com.centraldoalfabeto.game.domain.model.Jogador;
import com.centraldoalfabeto.game.dto.StudentProgressDTO;
import com.centraldoalfabeto.game.domain.model.TotalAudioReproductions;
import com.centraldoalfabeto.game.domain.model.TotalErrors;
import com.centraldoalfabeto.game.repository.EducadorRepository;
import com.centraldoalfabeto.game.repository.JogadorRepository;
import com.centraldoalfabeto.game.repository.TotalErrorsRepository;
import com.centraldoalfabeto.game.repository.TotalAudioReproductionsRepository;
import com.centraldoalfabeto.game.service.EducadorService;
import com.centraldoalfabeto.game.security.JwtAuthenticatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/educators")
public class EducadorController {
    @Autowired
    private EducadorService educadorService;

    @Autowired
    private EducadorRepository educadorRepository;

    @Autowired
    private JogadorRepository jogadorRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TotalErrorsRepository totalErrorsRepository;
    
    @Autowired
    private TotalAudioReproductionsRepository totalAudioReproductionsRepository;

    @PostMapping("/register")
    public ResponseEntity<Void> registerEducator(@RequestBody Educador educator) {
        if (educator.getEmail() == null || educator.getFullName() == null || educator.getSenha() == null) {
            return ResponseEntity.badRequest().build();
        }

        Optional<Educador> existingEducator = educadorRepository.findByEmail(educator.getEmail());
        if (existingEducator.isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        
        String encodedPassword = passwordEncoder.encode(educator.getSenha());
        educator.setSenha(encodedPassword);
        
        educadorService.save(educator);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/student-progress")
    public ResponseEntity<StudentProgressDTO> getStudentProgress(
            @RequestBody Jogador student,
            @AuthenticationPrincipal JwtAuthenticatedUser authenticatedUser) {

        if (authenticatedUser == null || !"EDUCATOR".equalsIgnoreCase(authenticatedUser.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (student.getId() == null) {
            return ResponseEntity.badRequest().build();
        }

        Optional<Educador> optionalEducador = educadorRepository.findById(authenticatedUser.getUserId());
        if (optionalEducador.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Educador educator = optionalEducador.get();
        Set<Long> allowedStudents = educator.getStudentIds();
        if (allowedStudents == null || !allowedStudents.contains(student.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Optional<Jogador> optionalJogador = jogadorRepository.findById(student.getId());

        if (optionalJogador.isPresent()) {
            Jogador jogador = optionalJogador.get();
            StudentProgressDTO progressDTO = new StudentProgressDTO();
            progressDTO.setCurrentPhaseIndex(jogador.getCurrentPhaseIndex());

            List<TotalErrors> errors = totalErrorsRepository.findErrorsByJogadorId(jogador.getId());

            Map<Integer, Integer> errorsByPhase = errors.stream()
                .collect(Collectors.toMap(TotalErrors::getCurrentPhaseIndex, TotalErrors::getValue));
            progressDTO.setNumberOfErrorsByPhase(errorsByPhase);

            List<TotalAudioReproductions> soundRepeats = totalAudioReproductionsRepository.findSoundRepeatsByJogadorId(jogador.getId());

            Map<Integer, Integer> soundRepeatsByPhase = soundRepeats.stream()
                .collect(Collectors.toMap(TotalAudioReproductions::getCurrentPhaseIndex, TotalAudioReproductions::getValue));
            progressDTO.setNumberOfSoundRepeatsByPhase(soundRepeatsByPhase);
            
            return ResponseEntity.ok(progressDTO);
        }

        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}/updateStudentIds")
    public ResponseEntity<Educador> updateStudentIds(
            @PathVariable Long id,
            @RequestBody Set<Long> studentIds,
            @AuthenticationPrincipal JwtAuthenticatedUser authenticatedUser) {

        if (authenticatedUser == null
                || !"EDUCATOR".equalsIgnoreCase(authenticatedUser.getRole())
                || !authenticatedUser.getUserId().equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Optional<Educador> optionalEducator = educadorRepository.findById(id);
        if (optionalEducator.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Educador educator = optionalEducator.get();
        educator.setStudentIds(studentIds);

        Educador updatedEducator = educadorService.save(educator);
        return ResponseEntity.ok(updatedEducator);
    }
}
