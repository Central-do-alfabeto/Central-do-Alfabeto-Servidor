package com.centraldoalfabeto.game.domain.controller;

import com.centraldoalfabeto.game.domain.model.Educador;
import com.centraldoalfabeto.game.domain.model.Jogador;
import com.centraldoalfabeto.game.dto.StudentProgressDTO;
import com.centraldoalfabeto.game.repository.EducadorRepository;
import com.centraldoalfabeto.game.repository.JogadorRepository;
import com.centraldoalfabeto.game.service.EducadorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/educators")
public class EducadorController {

    @Autowired
    private EducadorService educadorService;

    @Autowired
    private EducadorRepository educadorRepository;

    @Autowired
    private JogadorRepository jogadorRepository;

    @PostMapping("/register")
    public ResponseEntity<Void> registerEducator(@RequestBody Educador educator) {
        if (educator.getEmail() == null || educator.getFullName() == null || educator.getPassword() == null) {
            return ResponseEntity.badRequest().build();
        }

        Optional<Educador> existingEducator = educadorRepository.findByEmail(educator.getEmail());
        if (existingEducator.isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        
        educadorService.save(educator);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/student-progress")
    public ResponseEntity<StudentProgressDTO> getStudentProgress(@RequestBody Jogador student) {
        if (student.getId() == null) {
            return ResponseEntity.badRequest().build();
        }

        Optional<Jogador> optionalJogador = jogadorRepository.findById(student.getId());

        if (optionalJogador.isPresent()) {
            Jogador jogador = optionalJogador.get();

            StudentProgressDTO progressDTO = new StudentProgressDTO();
            progressDTO.setCurrentPhaseIndex(jogador.getCurrentPhaseIndex());
            progressDTO.setNumberOfErrorsByPhase(jogador.getNumberOfErrorsByPhase());
            progressDTO.setNumberOfSoundRepeatsByPhase(jogador.getNumberOfSoundRepeatsByPhase());

            return ResponseEntity.ok(progressDTO);
        }

        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}/updateStudentIds")
    public ResponseEntity<Educador> updateStudentIds(
            @PathVariable Long id,
            @RequestBody Set<Long> studentIds) {

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
