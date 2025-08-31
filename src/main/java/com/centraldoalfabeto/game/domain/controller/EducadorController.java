package com.centraldoalfabeto.game.domain.controller;

import com.centraldoalfabeto.game.domain.model.Educador;
import com.centraldoalfabeto.repository.EducadorRepository;
import com.centraldoalfabeto.service.EducadorService;
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
    private EducadorRepository educadorRepository; // "Adicionado para demonstração do método de atualização" de acordo com o Gemini

    @PostMapping("/register")
    public ResponseEntity<Educador> registerEducator(@RequestBody Educador educator) {
        Educador newEducator = educadorService.save(educator);
        return new ResponseEntity<>(newEducator, HttpStatus.CREATED);
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
