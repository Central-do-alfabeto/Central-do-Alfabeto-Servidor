-- Schema inicial derivado das entidades JPA (User, PlayerData, EducatorStudentLink).

CREATE TABLE users (
    id          UUID         NOT NULL,
    nome        VARCHAR(255),
    email       VARCHAR(255),
    senha_hash  VARCHAR(255),
    metadados   JSONB,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE player_data (
    id                  UUID    NOT NULL,
    players_id          UUID    NOT NULL,
    phase_index         INTEGER NOT NULL,
    erros_totais        BIGINT,
    reproducoes_totais  BIGINT,
    CONSTRAINT pk_player_data PRIMARY KEY (id),
    CONSTRAINT fk_player_data_user FOREIGN KEY (players_id) REFERENCES users (id),
    CONSTRAINT uk_player_data_player_phase UNIQUE (players_id, phase_index)
);

CREATE TABLE educators_students (
    educator_id  UUID NOT NULL,
    student_id   UUID NOT NULL,
    CONSTRAINT pk_educators_students PRIMARY KEY (educator_id, student_id),
    CONSTRAINT fk_educators_students_educator FOREIGN KEY (educator_id) REFERENCES users (id),
    CONSTRAINT fk_educators_students_student FOREIGN KEY (student_id) REFERENCES users (id)
);

CREATE INDEX idx_educators_students_student ON educators_students (student_id);
