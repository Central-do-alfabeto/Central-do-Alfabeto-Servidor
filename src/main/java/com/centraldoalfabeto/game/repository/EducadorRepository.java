package com.centraldoalfabeto.game.repository;

import com.centraldoalfabeto.game.domain.model.Educador;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface EducadorRepository extends JpaRepository<Educador, UUID> {
}
