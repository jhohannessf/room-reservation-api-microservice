CREATE TABLE codigos_a2f
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT     NOT NULL,
    codigo     VARCHAR(6) NOT NULL,
    expiracao  DATETIME   NOT NULL,
    utilizado  BOOLEAN    NOT NULL DEFAULT FALSE
);