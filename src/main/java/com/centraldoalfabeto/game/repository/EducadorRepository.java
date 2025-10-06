package com.centraldoalfabeto.game.repository;

import com.centraldoalfabeto.game.domain.model.Educador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID; 
import java.util.Optional;

public interface EducadorRepository extends JpaRepository<Educador, UUID> {
}
