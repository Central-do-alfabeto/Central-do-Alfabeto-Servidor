package com.centraldoalfabeto.game.service;

import com.centraldoalfabeto.game.domain.model.Educador;
import com.centraldoalfabeto.game.repository.EducadorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EducadorService {
    @Autowired
    private EducadorRepository educadorRepository;

    public Educador save(Educador educador) {
        return educadorRepository.save(educador);
    }
}
