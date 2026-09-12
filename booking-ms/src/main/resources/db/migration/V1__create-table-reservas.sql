CREATE TABLE reservas(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    data DATE NOT NULL,
    hora_inicial TIME NOT NULL,
    hora_final TIME NOT NULL,
    quantidade_pessoas INT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ATIVA',
    usuario_id BIGINT NOT NULL,
    sala_id BIGINT NOT NULL
);

