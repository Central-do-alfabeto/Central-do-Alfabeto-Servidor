package com.centraldoalfabeto.game.repository;

import com.centraldoalfabeto.game.domain.model.Jogador;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JogadorRepository extends JpaRepository<Jogador, Long> {
}
