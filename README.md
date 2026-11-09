# EntreAmigos — Gestión y control de gastos compartidos

App Android para dividir gastos en grupos (viajes, casas compartidas, salidas): registra
quién pagó, reparte en partes iguales y calcula quién le debe a quién con la menor cantidad
de transferencias posible. Funciona sin conexión y se sincroniza con un backend propio.

**Versión actual:** `0.75-beta` · Fase 3 de 4 (≈ 75 %)

## Integrantes
- Rodrigo Gino Alejandro Castillo
- Jose Moises Alvines Villegas
- Marycielo Mengoa Oliveros
- Maciel Bresia Contreras Yauri

---

## Progreso del proyecto

| Fase | Semanas | Alcance | Avance | Estado |
|---|---|---|---|---|
| 1 | 1–4 | App local: grupos, gastos, división, saldos, liquidación, notificaciones locales, compartir | 25 % | Completada |
| 2 | 5–8 | Backend Ktor + PostgreSQL, sincronización offline-first, estados de carga/error/offline, UI fiel al mockup | 50 % | Completada |
| **3** | **9–12** | **Autenticación, grupos compartidos entre dispositivos, edición/borrado, idempotencia** | **75 %** | **Completada** |
| 4 | 13–16 | Pulido, pruebas de UI, despliegue y presentación como producto terminado | 100 % | Pendiente |

### Funcionalidades

| Funcionalidad | Fase | Estado |
|---|---|---|
| Crear grupos con integrantes e ícono | 1 | ✔ |
| Registrar gastos y dividirlos en partes iguales (sin perder céntimos) | 1 → 2 | ✔ (corregido en Fase 2) |
| Saldos netos y deudas simplificadas | 1 | ✔ |
| Marcar deudas como pagadas | 1 | ✔ |
| Estadísticas por categoría | 1 | ✔ |
| Notificación local al registrar un gasto | 1 | ✔ |
| Compartir resumen del grupo (Intent) | 1 | ✔ |
| API REST con PostgreSQL en Docker | 2 | ✔ |
| Sincronización offline-first (write-through + pull con upsert) | 2 | ✔ |
| Indicador de modo offline, banner de error y reintento | 2 | ✔ |
| Pull-to-refresh | 2 | ✔ |
| Menú lateral, campana con gastos no vistos, saludo con nombre | 2 | ✔ |
| Iconografía vectorial (sin emojis) | 2 | ✔ |
| Inicio de sesión y registro (JWT) | 3 | ✔ |
| Inicio de sesión con Google (Firebase Authentication) | 3 | ✔ (requiere configurar Firebase) |
| Usuarios semilla del equipo + grupo demo (`seed.sql`) | 3 | ✔ |
| Grupos compartidos entre dispositivos (código de invitación) | 3 | ✔ |
| Editar / eliminar gastos (también offline) | 3 | ✔ |
| Idempotencia en los POST (`Idempotency-Key`) | 3 | ✔ |
| Cerrar sesión (borra los datos locales de la cuenta) | 3 | ✔ |

---

## Cuentas de prueba

Al levantar la base por primera vez el backend carga
[`backend/src/main/resources/db/seed.sql`](backend/src/main/resources/db/seed.sql): una cuenta
por integrante y el grupo compartido **“Proyecto Desarrollo Móvil”** (código `DEMO26`) con
tres gastos de ejemplo.

| Integrante | Email | Contraseña |
|---|---|---|
| Rodrigo Gino Alejandro Castillo | `gino@entreamigos.test` | `EntreAmigos2026` |
| Jose Moises Alvines Villegas | `moises@entreamigos.test` | `EntreAmigos2026` |
| Marycielo Mengoa Oliveros | `marycielo@entreamigos.test` | `EntreAmigos2026` |
| Maciel Bresia Contreras Yauri | `maciel@entreamigos.test` | `EntreAmigos2026` |

- El seed corre automáticamente justo después de las migraciones de Flyway, **solo si la tabla
  `usuarios` está vacía** (base nueva). Así, reiniciar la API no “resucita” gastos de demo
  que alguien borró. Para desactivarlo: `DB_SEED=false` en `.env`.
- No se monta en `/docker-entrypoint-initdb.d` porque ahí Postgres lo ejecutaría antes de
  que Flyway cree las tablas.
- Para volver a empezar con datos limpios: `docker compose down -v && docker compose up -d`.
- Son credenciales de desarrollo: no usar este seed en un servidor público.

---

## Inicio de sesión con Google (Firebase)

Servicio de Google integrado: **Firebase Authentication** con el proveedor Google.

```
App ──(Credential Manager)──► cuenta de Google ──► ID token de Google
App ──signInWithCredential──► Firebase Auth ──► ID token de Firebase
App ──POST /auth/google {idToken}──► API (verifica firma, proyecto y expiración
                                      con las claves públicas de Google)
API ──► JWT propio de EntreAmigos (el mismo que el login con contraseña)
```

- La primera vez se crea la cuenta con el nombre y email de Google. Si ya existía una cuenta
  con ese email (verificado por Google), se vincula a ella.
- Cerrar sesión en la app también cierra la sesión de Firebase, para poder elegir otra cuenta.
- **Sin configurar Firebase todo sigue funcionando:** la app compila y el botón
  “Continuar con Google” simplemente no aparece (y `POST /auth/google` responde `503`).

### Pasos manuales (una sola vez)

1. **Crear el proyecto** en <https://console.firebase.google.com> → *Agregar proyecto*
   (Google Analytics es opcional).
2. **Registrar la app Android** (ícono de Android en la página del proyecto):
   - Nombre del paquete: `com.entreamigos.app`
   - **SHA-1 del certificado de depuración** (obligatorio para el login con Google).
     Se obtiene con `./gradlew signingReport` (variante `debug`, línea `SHA1`) o con
     `keytool -list -v -keystore %USERPROFILE%\.android\debug.keystore -alias androiddebugkey -storepass android`.
     Cada integrante tiene su propio keystore de debug: agreguen **el SHA-1 de cada PC**
     en *Configuración del proyecto → Tus apps → Agregar huella digital*.
3. **Habilitar Google** en *Authentication → Comenzar → Sign-in method → Google →
   Habilitar*, elegir el correo de asistencia y guardar.
4. **Descargar `app/google-services.json`** (*Configuración del proyecto → Tus apps*) **después**
   del paso 3 (así incluye el *Web client ID*) y copiarlo en `app/google-services.json`.
   Está en `.gitignore`: compártanlo por un canal privado (o que cada uno lo descargue
   de la consola). Sin el archivo la app compila igual, pero sin el botón de Google.
5. **Configurar el backend:** en `.env` poner `FIREBASE_PROJECT_ID=<ID del proyecto>`
   (*Configuración del proyecto → General → ID del proyecto*) y ejecutar
   `docker compose up -d api`. El log debe decir *“Inicio de sesión con Google habilitado”*.
6. **Probar** en un dispositivo físico o en un emulador con imagen **Google Play** que tenga
   una cuenta de Google agregada (*Ajustes → Cuentas*).

Si aparece “No hay cuentas de Google disponibles” o un error `[28444]`/`DEVELOPER_ERROR`,
casi siempre falta el SHA-1 del equipo en Firebase o el `app/google-services.json` es anterior
a habilitar Google.

---


## Cómo levantar el backend

**Requisitos:** Docker Desktop (o Docker Engine + Compose v2).

```bash
# 1. Variables de entorno (opcional: sin .env se usan valores de desarrollo)
cp .env.example .env
#    edita POSTGRES_PASSWORD y, si 5432/8080 están ocupados, POSTGRES_PORT / API_PORT

# 2. Levantar base de datos + API
docker compose up -d

# 3. Verificar
curl http://localhost:8080/health          # {"estado":"ok"}
curl -X POST http://localhost:8080/auth/login -H "Content-Type: application/json" \
     -d '{"email":"gino@entreamigos.test","password":"EntreAmigos2026"}'   # {"token":"…"}
```

- La primera vez la imagen de la API tarda unos minutos en compilar; las siguientes usan caché.
- Las tablas se crean solas al arrancar la API (Flyway aplica `V1__esquema_inicial.sql` y
  `V2__autenticacion_e_idempotencia.sql`) y luego se cargan los datos semilla.
- Fuera de desarrollo define `JWT_SECRET` en `.env` con un valor largo y aleatorio.
- Los datos persisten en el volumen `entreamigos_db_data`.

| Acción | Comando |
|---|---|
| Ver logs de la API | `docker compose logs -f api` |
| Reconstruir tras cambiar el backend | `docker compose up -d --build api` |
| Detener (conserva datos) | `docker compose down` |
| Borrar también los datos | `docker compose down -v` |
| Consola SQL | `docker compose exec db psql -U entreamigos entreamigos` |

El contrato completo (endpoints, cuerpos, respuestas y códigos HTTP) está en
[`backend/API.md`](backend/API.md).

### Ejecutar el backend sin Docker (desarrollo)
Con un PostgreSQL local accesible:

```bash
cd backend
DB_URL=jdbc:postgresql://localhost:5432/entreamigos DB_USER=entreamigos DB_PASSWORD=entreamigos ./gradlew run
```

---

## Cómo ejecutar la app

1. Abrir el proyecto en Android Studio (JDK 17+, SDK 34).
2. Levantar el backend (sección anterior).
3. Ejecutar la configuración `app` en un emulador.

La app apunta a `http://10.0.2.2:8080/`, que es como el emulador ve el `localhost` de la
máquina. Para usar un **dispositivo físico** o un servidor remoto, agrega en
`local.properties` (no se versiona):

```properties
entreamigos.api.url.debug=http://192.168.1.50:8080/
entreamigos.api.url.release=https://api.tu-dominio.com/
```

También se puede pasar por línea de comandos:
`./gradlew assembleDebug -Pentreamigos.api.url.debug=http://192.168.1.50:8080/`.

En debug se permite HTTP sin cifrar; en release solo HTTPS (salvo `10.0.2.2`/`localhost`).

Si el backend no está levantado la app funciona igual en modo offline.

