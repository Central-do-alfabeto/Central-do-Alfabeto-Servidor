package com.centraldoalfabeto.game.repository;

import com.centraldoalfabeto.game.domain.model.Jogador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;
import java.util.Optional;

public interface JogadorRepository extends JpaRepository<Jogador, UUID> {
    @Query("SELECT e FROM Jogador e JOIN e.user u WHERE u.nome = :fullName")
    Optional<Jogador> findByFullName(@Param("fullName") String fullName);
}
