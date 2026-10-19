# EntreAmigos — Gestión y control de gastos compartidos

App Android para dividir gastos en grupos (viajes, casas compartidas, salidas): registra
quién pagó, reparte en partes iguales y calcula quién le debe a quién con la menor cantidad
de transferencias posible. Funciona sin conexión y se sincroniza con un backend propio.

**Versión actual:** `0.50-beta` · Fase 2 de 4 (≈ 50 %)

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
| **2** | **5–8** | **Backend Ktor + PostgreSQL, sincronización offline-first, estados de carga/error/offline, UI fiel al mockup** | **50 %** | **Completada** |
| 3 | 9–12 | Autenticación, grupos compartidos entre dispositivos, edición/borrado, idempotencia | 75 % | Pendiente |
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
| Inicio de sesión | 3 | Pendiente |
| Editar / eliminar gastos | 3 | Pendiente |

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
curl http://localhost:8080/grupos          # []
```

- La primera vez la imagen de la API tarda unos minutos en compilar; las siguientes usan caché.
- Las tablas se crean solas al arrancar la API (Flyway aplica `V1__esquema_inicial.sql`).
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

