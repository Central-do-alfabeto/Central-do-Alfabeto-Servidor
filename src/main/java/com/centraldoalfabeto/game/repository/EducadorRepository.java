package com.centraldoalfabeto.game.repository;

import com.centraldoalfabeto.game.domain.model.Educador;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EducadorRepository extends JpaRepository<Educador, Long> {
    Optional<Educador> findByEmail(String email);
}
