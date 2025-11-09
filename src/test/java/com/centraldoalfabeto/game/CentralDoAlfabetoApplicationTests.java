package com.centraldoalfabeto.game;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

import com.centraldoalfabeto.game.service.VoskTranscriptionService;

@SpringBootTest
@ComponentScan(excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = VoskTranscriptionService.class))
class CentralDoAlfabetoApplicationTests {
    @Test
    void contextLoads() {
    }
}