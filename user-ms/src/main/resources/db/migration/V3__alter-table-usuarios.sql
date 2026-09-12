ALTER TABLE usuarios
ADD COLUMN provedor_login VARCHAR(20);

UPDATE usuarios
SET usuarios.provedor_login = 'LOCAL'
WHERE usuarios.provedor_login IS NULL;

ALTER TABLE usuarios
MODIFY COLUMN provedor_login VARCHAR(20) NOT NULL;