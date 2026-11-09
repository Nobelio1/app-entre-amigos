-- ============================================================================
-- Datos semilla de EntreAmigos (solo desarrollo / demo)
--
-- El backend lo ejecuta automáticamente justo después de aplicar las migraciones
-- de Flyway, únicamente cuando la tabla `usuarios` está vacía (base recién
-- creada). Además cada sentencia es idempotente, así que ejecutarlo a mano de
-- nuevo no duplica nada:
--
--   docker compose exec -T db psql -U entreamigos entreamigos < backend/src/main/resources/db/seed.sql
--
-- Cuentas creadas (contraseña de todas: EntreAmigos2026):
--   gino@entreamigos.test       Rodrigo Gino Alejandro Castillo
--   moises@entreamigos.test     Jose Moises Alvines Villegas
--   marycielo@entreamigos.test  Marycielo Mengoa Oliveros
--   maciel@entreamigos.test     Maciel Bresia Contreras Yauri
-- ============================================================================

-- crypt()/gen_salt() generan hashes bcrypt ($2a$) compatibles con el backend
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ---------------------------------------------------------------------------
-- Usuarios: uno por integrante del proyecto
-- ---------------------------------------------------------------------------
INSERT INTO usuarios (nombre, email, password_hash, color_avatar_hex, fecha_creacion)
SELECT v.nombre, v.email, crypt('EntreAmigos2026', gen_salt('bf', 10)), v.color,
       (extract(epoch FROM now()) * 1000)::bigint
FROM (VALUES
    (1, 'Gino Castillo',    'gino@entreamigos.test',      '#EF5350'),
    (2, 'Moisés Alvines',   'moises@entreamigos.test',    '#42A5F5'),
    (3, 'Marycielo Mengoa', 'marycielo@entreamigos.test', '#66BB6A'),
    (4, 'Maciel Contreras', 'maciel@entreamigos.test',    '#AB47BC')
) AS v(orden, nombre, email, color)
WHERE NOT EXISTS (SELECT 1 FROM usuarios u WHERE lower(u.email) = lower(v.email))
ORDER BY v.orden;

-- ---------------------------------------------------------------------------
-- Grupo compartido de demostración con los cuatro integrantes
-- (cualquiera puede invitar a más gente con el código DEMO26)
-- ---------------------------------------------------------------------------
INSERT INTO grupos (nombre, icono, color_hex, fecha_creacion, creado_por, codigo_invitacion, idempotency_key)
SELECT 'Proyecto Desarrollo Móvil', 'ESTUDIOS', '#1B5E4F',
       (extract(epoch FROM now() - interval '7 days') * 1000)::bigint,
       u.id, 'DEMO26', 'seed-grupo-proyecto'
FROM usuarios u
WHERE lower(u.email) = 'gino@entreamigos.test'
ON CONFLICT DO NOTHING;

INSERT INTO miembros (grupo_id, nombre, color_avatar_hex, usuario_id, idempotency_key)
SELECT g.id, u.nombre, u.color_avatar_hex, u.id, 'seed-miembro-' || split_part(u.email, '@', 1)
FROM grupos g
JOIN (VALUES
    (1, 'gino@entreamigos.test'),
    (2, 'moises@entreamigos.test'),
    (3, 'marycielo@entreamigos.test'),
    (4, 'maciel@entreamigos.test')
) AS v(orden, email) ON TRUE
JOIN usuarios u ON lower(u.email) = v.email
WHERE g.idempotency_key = 'seed-grupo-proyecto'
ORDER BY v.orden
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------
-- Gastos de ejemplo (el reparto suma exactamente el monto de cada gasto)
-- ---------------------------------------------------------------------------
INSERT INTO gastos (grupo_id, descripcion, monto, pagado_por_id, categoria, fecha, idempotency_key)
SELECT g.id, v.descripcion, v.monto, m.id, v.categoria,
       (extract(epoch FROM now() - v.hace) * 1000)::bigint, v.clave
FROM (VALUES
    ('seed-gasto-informe',  'Impresión y anillado del informe', 48.00,  'gino@entreamigos.test',      'OTROS',     interval '5 days'),
    ('seed-gasto-almuerzo', 'Almuerzo después del avance',      120.00, 'marycielo@entreamigos.test', 'COMIDA',    interval '3 days'),
    ('seed-gasto-hosting',  'Hosting de la API (1 mes)',        35.50,  'moises@entreamigos.test',    'SERVICIOS', interval '1 day')
) AS v(clave, descripcion, monto, pagador, categoria, hace)
JOIN grupos g   ON g.idempotency_key = 'seed-grupo-proyecto'
JOIN usuarios u ON lower(u.email) = v.pagador
JOIN miembros m ON m.grupo_id = g.id AND m.usuario_id = u.id
ON CONFLICT DO NOTHING;

INSERT INTO gasto_participantes (gasto_id, miembro_id, monto_asignado)
SELECT ga.id, m.id, v.monto_asignado
FROM (VALUES
    ('seed-gasto-informe',  'gino@entreamigos.test',      12.00),
    ('seed-gasto-informe',  'moises@entreamigos.test',    12.00),
    ('seed-gasto-informe',  'marycielo@entreamigos.test', 12.00),
    ('seed-gasto-informe',  'maciel@entreamigos.test',    12.00),
    ('seed-gasto-almuerzo', 'gino@entreamigos.test',      30.00),
    ('seed-gasto-almuerzo', 'moises@entreamigos.test',    30.00),
    ('seed-gasto-almuerzo', 'marycielo@entreamigos.test', 30.00),
    ('seed-gasto-almuerzo', 'maciel@entreamigos.test',    30.00),
    ('seed-gasto-hosting',  'gino@entreamigos.test',      17.75),
    ('seed-gasto-hosting',  'moises@entreamigos.test',    17.75)
) AS v(clave, email, monto_asignado)
JOIN gastos ga  ON ga.idempotency_key = v.clave
JOIN usuarios u ON lower(u.email) = v.email
JOIN miembros m ON m.grupo_id = ga.grupo_id AND m.usuario_id = u.id
ON CONFLICT DO NOTHING;
