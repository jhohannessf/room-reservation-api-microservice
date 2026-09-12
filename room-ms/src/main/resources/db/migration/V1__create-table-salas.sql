CREATE TABLE salas(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    numero INT NOT NULL UNIQUE,
    capacidade INT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'LIVRE',
    CONSTRAINT chk_capacidade_positiva CHECK (capacidade > 0)
);