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
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        educadorService.save(educator);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PostMapping("/student-progress")
    public ResponseEntity<StudentProgressDTO> getStudentProgress(@RequestBody Jogador student) {
        if (student.getId() == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        Optional<Jogador> optionalJogador = jogadorRepository.findById(student.getId());

        if (optionalJogador.isPresent()) {
            Jogador jogador = optionalJogador.get();
            StudentProgressDTO progressDTO = new StudentProgressDTO();
            progressDTO.setCurrentPhaseIndex(jogador.getCurrentPhaseIndex());
            progressDTO.setNumberOfErrorsByPhase(jogador.getNumberOfErrorsByPhase());
            progressDTO.setNumberOfSoundRepeatsByPhase(jogador.getNumberOfSoundRepeatsByPhase());
            
            return new ResponseEntity<>(progressDTO, HttpStatus.OK);
        }

        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @PutMapping("/{id}/updateStudentIds")
    public ResponseEntity<Educador> updateStudentIds(
            @PathVariable Long id,
            @RequestBody Set<Long> studentIds) {

        Optional<Educador> optionalEducator = educadorRepository.findById(id);
        if (optionalEducator.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Educador educator = optionalEducator.get();
        educator.setStudentIds(studentIds);

        Educador updatedEducator = educadorService.save(educator);
        return new ResponseEntity<>(updatedEducator, HttpStatus.OK);
    }
}
