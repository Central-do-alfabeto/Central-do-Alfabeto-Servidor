package com.centraldoalfabeto.game.service;

import com.centraldoalfabeto.game.domain.model.Jogador;
import com.centraldoalfabeto.game.repository.JogadorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class JogadorService {
    @Autowired
    private JogadorRepository jogadorRepository;

    public Jogador salvar(Jogador jogador) {
        return jogadorRepository.save(jogador);
    }
}
