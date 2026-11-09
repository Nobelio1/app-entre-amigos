-- Fase 3: usuarios, grupos compartidos, idempotencia y edición de gastos

CREATE TABLE usuarios (
    id                BIGSERIAL PRIMARY KEY,
    nombre            VARCHAR(80)  NOT NULL CHECK (length(trim(nombre)) > 0),
    email             VARCHAR(160) NOT NULL,
    password_hash     VARCHAR(100) NOT NULL,
    color_avatar_hex  VARCHAR(9)   NOT NULL DEFAULT '#7E57C2',
    fecha_creacion    BIGINT       NOT NULL
);
-- El email se compara sin distinguir mayúsculas
CREATE UNIQUE INDEX ux_usuarios_email ON usuarios (lower(email));

-- Un miembro puede estar vinculado a una cuenta (el resto son "invitados" sin cuenta)
ALTER TABLE miembros ADD COLUMN usuario_id BIGINT REFERENCES usuarios(id) ON DELETE SET NULL;
CREATE UNIQUE INDEX ux_miembros_grupo_usuario ON miembros (grupo_id, usuario_id) WHERE usuario_id IS NOT NULL;
CREATE INDEX idx_miembros_usuario ON miembros (usuario_id);

-- "es_usuario_actual" ahora se deriva del usuario autenticado
ALTER TABLE miembros DROP COLUMN es_usuario_actual;

ALTER TABLE grupos ADD COLUMN creado_por BIGINT REFERENCES usuarios(id) ON DELETE SET NULL;
ALTER TABLE grupos ADD COLUMN codigo_invitacion VARCHAR(12);
UPDATE grupos SET codigo_invitacion = upper(substr(md5(random()::text || id::text), 1, 6));
ALTER TABLE grupos ALTER COLUMN codigo_invitacion SET NOT NULL;
CREATE UNIQUE INDEX ux_grupos_codigo ON grupos (codigo_invitacion);

-- Idempotency-Key: un reintento del mismo POST devuelve el recurso ya creado
ALTER TABLE grupos   ADD COLUMN idempotency_key VARCHAR(64);
ALTER TABLE miembros ADD COLUMN idempotency_key VARCHAR(64);
ALTER TABLE gastos   ADD COLUMN idempotency_key VARCHAR(64);
ALTER TABLE pagos    ADD COLUMN idempotency_key VARCHAR(64);
CREATE UNIQUE INDEX ux_grupos_idem   ON grupos   (idempotency_key) WHERE idempotency_key IS NOT NULL;
CREATE UNIQUE INDEX ux_miembros_idem ON miembros (idempotency_key) WHERE idempotency_key IS NOT NULL;
CREATE UNIQUE INDEX ux_gastos_idem   ON gastos   (idempotency_key) WHERE idempotency_key IS NOT NULL;
CREATE UNIQUE INDEX ux_pagos_idem    ON pagos    (idempotency_key) WHERE idempotency_key IS NOT NULL;

ALTER TABLE gastos ADD COLUMN fecha_actualizacion BIGINT;
