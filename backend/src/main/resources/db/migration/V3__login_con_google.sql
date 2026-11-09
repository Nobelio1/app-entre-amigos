-- Fase 3: inicio de sesión con Google (Firebase Authentication)

-- Las cuentas creadas con Google no tienen contraseña propia
ALTER TABLE usuarios ALTER COLUMN password_hash DROP NOT NULL;

-- UID de Firebase (claim "sub" del ID token); vincula la cuenta de Google con el usuario
ALTER TABLE usuarios ADD COLUMN firebase_uid VARCHAR(128);
CREATE UNIQUE INDEX ux_usuarios_firebase_uid ON usuarios (firebase_uid) WHERE firebase_uid IS NOT NULL;

ALTER TABLE usuarios ADD COLUMN foto_url VARCHAR(500);
