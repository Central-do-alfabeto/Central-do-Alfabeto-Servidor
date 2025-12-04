package com.centraldoalfabeto.game.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class PlayerDataSchemaNormalizer {
    private static final Logger logger = LoggerFactory.getLogger(PlayerDataSchemaNormalizer.class);
    private final JdbcTemplate jdbcTemplate;

    public PlayerDataSchemaNormalizer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void normalizePlayerDataPrimaryKey() {
        try {
            Boolean hasUppercaseColumn = jdbcTemplate.queryForObject(
                """
                    SELECT EXISTS (
                        SELECT 1
                        FROM information_schema.columns
                        WHERE table_schema = current_schema()
                          AND table_name = 'player_data'
                          AND column_name = 'Id'
                    )
                """,
                Boolean.class
            );

            Boolean hasLowercaseColumn = jdbcTemplate.queryForObject(
                """
                    SELECT EXISTS (
                        SELECT 1
                        FROM information_schema.columns
                        WHERE table_schema = current_schema()
                          AND table_name = 'player_data'
                          AND column_name = 'id'
                    )
                """,
                Boolean.class
            );

            if (Boolean.TRUE.equals(hasUppercaseColumn) && !Boolean.TRUE.equals(hasLowercaseColumn)) {
                jdbcTemplate.execute("ALTER TABLE player_data RENAME COLUMN \"Id\" TO id");
                logger.info("Renamed player_data primary key column from \"Id\" to id.");
            }
        } catch (DataAccessException ex) {
            logger.warn("Não foi possível ajustar a coluna primária da tabela player_data automaticamente.", ex);
        }
    }
}
