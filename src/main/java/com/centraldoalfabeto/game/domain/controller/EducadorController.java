package com.centraldoalfabeto.game.domain.controller;

import com.centraldoalfabeto.game.dto.AddStudentRequestDTO;
import com.centraldoalfabeto.game.dto.EducatorRegistrationDTO;
import com.centraldoalfabeto.game.dto.StudentProgressDTO;
import com.centraldoalfabeto.game.dto.StudentSummaryDTO;
import com.centraldoalfabeto.game.dto.UnifiedLoginResponseDTO;
import com.centraldoalfabeto.game.service.EducadorService;
import com.centraldoalfabeto.game.security.JwtAuthenticatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/educators")
public class EducadorController {
    private final EducadorService educadorService;

    @Autowired
    public EducadorController(EducadorService educadorService) {
        this.educadorService = educadorService;
    }

    @PostMapping("/register")
    public ResponseEntity<UnifiedLoginResponseDTO> registerEducator(@RequestBody EducatorRegistrationDTO registrationDTO) {
        
        if (registrationDTO.getEmail() == null || registrationDTO.getNome() == null || registrationDTO.getSenha() == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            UnifiedLoginResponseDTO responseDTO = educadorService.registerEducator(registrationDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
        } catch (IllegalArgumentException e) {
            // Exemplo: Email já existe
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}/students")
    public ResponseEntity<List<StudentSummaryDTO>> listStudents(
            @PathVariable UUID id,
            @AuthenticationPrincipal JwtAuthenticatedUser authenticatedUser
    ) {
        if (authenticatedUser == null
                || !"EDUCATOR".equalsIgnoreCase(authenticatedUser.getRole())
                || !authenticatedUser.getUserId().equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            List<StudentSummaryDTO> students = educadorService.listStudents(id);
            return ResponseEntity.ok(students);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{id}/students")
    public ResponseEntity<StudentSummaryDTO> addStudent(
            @PathVariable UUID id,
            @RequestBody AddStudentRequestDTO request,
            @AuthenticationPrincipal JwtAuthenticatedUser authenticatedUser
    ) {
        if (authenticatedUser == null
                || !"EDUCATOR".equalsIgnoreCase(authenticatedUser.getRole())
                || !authenticatedUser.getUserId().equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (request == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            StudentSummaryDTO student = educadorService.addStudent(id, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(student);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/student-progress/{studentId}")
    public ResponseEntity<StudentProgressDTO> getStudentProgress(
            @PathVariable UUID studentId,
            @AuthenticationPrincipal JwtAuthenticatedUser authenticatedUser) {

        if (authenticatedUser == null || !"EDUCATOR".equalsIgnoreCase(authenticatedUser.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            StudentProgressDTO progressDTO = educadorService.getStudentProgress(
                authenticatedUser.getUserId(), 
                studentId
            );
            return ResponseEntity.ok(progressDTO);
            
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); 
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/updateStudentIds")
    public ResponseEntity<Set<UUID>> updateStudentIds(
            @PathVariable UUID id,
            @RequestBody Set<UUID> studentIds,
            @AuthenticationPrincipal JwtAuthenticatedUser authenticatedUser) {

        if (authenticatedUser == null
                || !"EDUCATOR".equalsIgnoreCase(authenticatedUser.getRole())
                || !authenticatedUser.getUserId().equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        try {
            Set<UUID> updatedStudentIds = educadorService.updateStudentIds(id, studentIds);
            return ResponseEntity.ok(updatedStudentIds);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
