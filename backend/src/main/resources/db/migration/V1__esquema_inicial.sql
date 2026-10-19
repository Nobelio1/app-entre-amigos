-- Esquema inicial de EntreAmigos.
-- Es un port 1:1 de las entidades Room de la app Android (data/local/entity),
-- que siguen siendo la fuente de verdad del modelo. Diferencias deliberadas:
--   * Los montos usan NUMERIC(12,2) en vez de REAL para no acumular error de
--     punto flotante en el servidor.
--   * Las fechas se guardan como epoch en milisegundos (BIGINT), igual que en
--     Room, para que el cliente no tenga que convertir zonas horarias.

CREATE TABLE grupos (
    id              BIGSERIAL PRIMARY KEY,
    nombre          VARCHAR(120) NOT NULL CHECK (length(trim(nombre)) > 0),
    icono           VARCHAR(40)  NOT NULL DEFAULT 'GENERAL',
    color_hex       VARCHAR(9)   NOT NULL DEFAULT '#1B5E4F',
    fecha_creacion  BIGINT       NOT NULL
);

CREATE TABLE miembros (
    id                 BIGSERIAL PRIMARY KEY,
    grupo_id           BIGINT       NOT NULL REFERENCES grupos(id) ON DELETE CASCADE,
    nombre             VARCHAR(80)  NOT NULL CHECK (length(trim(nombre)) > 0),
    es_usuario_actual  BOOLEAN      NOT NULL DEFAULT FALSE,
    color_avatar_hex   VARCHAR(9)   NOT NULL DEFAULT '#7E57C2'
);
CREATE INDEX idx_miembros_grupo ON miembros(grupo_id);

CREATE TABLE gastos (
    id              BIGSERIAL PRIMARY KEY,
    grupo_id        BIGINT         NOT NULL REFERENCES grupos(id) ON DELETE CASCADE,
    descripcion     VARCHAR(200)   NOT NULL CHECK (length(trim(descripcion)) > 0),
    monto           NUMERIC(12, 2) NOT NULL CHECK (monto > 0),
    pagado_por_id   BIGINT         NOT NULL REFERENCES miembros(id) ON DELETE CASCADE,
    categoria       VARCHAR(30)    NOT NULL DEFAULT 'OTROS'
        CHECK (categoria IN ('COMIDA', 'TRANSPORTE', 'HOSPEDAJE', 'ENTRETENIMIENTO', 'SERVICIOS', 'OTROS')),
    fecha           BIGINT         NOT NULL
);
CREATE INDEX idx_gastos_grupo ON gastos(grupo_id);
CREATE INDEX idx_gastos_pagado_por ON gastos(pagado_por_id);

CREATE TABLE gasto_participantes (
    id              BIGSERIAL PRIMARY KEY,
    gasto_id        BIGINT         NOT NULL REFERENCES gastos(id) ON DELETE CASCADE,
    miembro_id      BIGINT         NOT NULL REFERENCES miembros(id) ON DELETE CASCADE,
    monto_asignado  NUMERIC(12, 2) NOT NULL CHECK (monto_asignado >= 0),
    UNIQUE (gasto_id, miembro_id)
);
CREATE INDEX idx_participantes_gasto ON gasto_participantes(gasto_id);
CREATE INDEX idx_participantes_miembro ON gasto_participantes(miembro_id);

CREATE TABLE pagos (
    id           BIGSERIAL PRIMARY KEY,
    grupo_id     BIGINT         NOT NULL REFERENCES grupos(id) ON DELETE CASCADE,
    deudor_id    BIGINT         NOT NULL REFERENCES miembros(id) ON DELETE CASCADE,
    acreedor_id  BIGINT         NOT NULL REFERENCES miembros(id) ON DELETE CASCADE,
    monto        NUMERIC(12, 2) NOT NULL CHECK (monto > 0),
    fecha        BIGINT         NOT NULL,
    CHECK (deudor_id <> acreedor_id)
);
CREATE INDEX idx_pagos_grupo ON pagos(grupo_id);
CREATE INDEX idx_pagos_deudor ON pagos(deudor_id);
CREATE INDEX idx_pagos_acreedor ON pagos(acreedor_id);
