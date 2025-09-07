package com.centraldoalfabeto.game.repository;

import com.centraldoalfabeto.game.domain.model.Jogador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JogadorRepository extends JpaRepository<Jogador, Long> {
	Optional<Jogador> findByFullName(String full_name);
}
