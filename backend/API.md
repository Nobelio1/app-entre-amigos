# API REST de EntreAmigos

Backend Ktor + PostgreSQL. Base URL local: `http://localhost:8080`
(desde el emulador de Android: `http://10.0.2.2:8080`).

## Convenciones

| Aspecto | Regla |
|---|---|
| Formato | JSON UTF-8 (`Content-Type: application/json`) |
| Montos | Número decimal en soles con hasta 2 decimales (`120.5`). Internamente se guardan como `NUMERIC(12,2)` y se calculan en céntimos enteros. |
| Fechas | Epoch en **milisegundos** (`1716508800000`), igual que en Room. |
| IDs | Enteros `Long` generados por el servidor. |
| Categorías | `COMIDA`, `TRANSPORTE`, `HOSPEDAJE`, `ENTRETENIMIENTO`, `SERVICIOS`, `OTROS` (no distingue mayúsculas en la entrada). |
| Íconos de grupo | Texto libre en mayúsculas que la app mapea a su enum `GrupoIcono` (`VIAJE`, `CASA`, `SALIDAS`, …). |
| Autenticación | Ninguna en esta fase (se agrega en la Fase 3). |

### Formato de error

Todas las respuestas de error usan el mismo cuerpo:

```json
{ "error": "Mensaje legible para el usuario o desarrollador" }
```

| Código | Cuándo |
|---|---|
| `200 OK` | Lectura exitosa |
| `201 Created` | Recurso creado; el cuerpo contiene el recurso completo con su `id` |
| `400 Bad Request` | JSON mal formado, campos obligatorios ausentes, validación fallida o id no numérico |
| `404 Not Found` | El grupo no existe o la ruta no existe |
| `500 Internal Server Error` | Error inesperado. El mensaje es genérico; el detalle queda en los logs del contenedor (`docker compose logs api`) |

---

## Salud

### `GET /health`
`200` → `{ "estado": "ok" }`

---

## Grupos

### `GET /grupos`
Lista todos los grupos con sus miembros, del más reciente al más antiguo.

`200` →
```json
[
  {
    "id": 1,
    "nombre": "Viaje a Punta Cana",
    "icono": "VIAJE",
    "colorHex": "#1B5E4F",
    "fechaCreacion": 1716508800000,
    "miembros": [
      { "id": 1, "grupoId": 1, "nombre": "Moisés", "esUsuarioActual": true, "colorAvatarHex": "#EF5350" },
      { "id": 2, "grupoId": 1, "nombre": "Ana", "esUsuarioActual": false, "colorAvatarHex": "#42A5F5" }
    ]
  }
]
```

### `POST /grupos`
Crea un grupo con sus miembros en una sola transacción.

| Campo | Tipo | Obligatorio | Notas |
|---|---|---|---|
| `nombre` | string | sí | 1–120 caracteres |
| `icono` | string | no | Por defecto `GENERAL` |
| `colorHex` | string | no | `#RRGGBB`, por defecto `#1B5E4F` |
| `miembros` | array | sí | Mínimo 2. Cada uno: `nombre` (obligatorio, 1–80), `esUsuarioActual` (bool, máx. uno en `true`), `colorAvatarHex` (opcional; si falta se asigna de la paleta) |
| `fechaCreacion` | long | no | Para grupos creados offline; por defecto la hora del servidor |

```json
{
  "nombre": "Viaje a Punta Cana",
  "icono": "VIAJE",
  "colorHex": "#1B5E4F",
  "miembros": [ { "nombre": "Moisés", "esUsuarioActual": true }, { "nombre": "Ana" } ]
}
```
`201` → el grupo creado (mismo formato que en `GET /grupos`). `400` si falla alguna validación.

### `GET /grupos/{id}`
`200` → un grupo con sus miembros. `404` si no existe. `400` si `id` no es un entero positivo.

### `POST /grupos/{id}/miembros`
Agrega un miembro a un grupo existente.

```json
{ "nombre": "Carlos", "esUsuarioActual": false, "colorAvatarHex": "#AB47BC" }
```
`201` →
```json
{ "id": 4, "grupoId": 1, "nombre": "Carlos", "esUsuarioActual": false, "colorAvatarHex": "#AB47BC" }
```
`400` si el nombre está vacío, ya existe en el grupo (sin distinguir mayúsculas) o ya hay un usuario actual. `404` si el grupo no existe.

---

## Gastos

### `GET /grupos/{id}/gastos`
Gastos del grupo, del más reciente al más antiguo, con el reparto entre participantes.

`200` →
```json
[
  {
    "id": 1,
    "grupoId": 1,
    "descripcion": "Cena en restaurante",
    "monto": 100.0,
    "pagadoPorId": 2,
    "categoria": "COMIDA",
    "fecha": 1716508800000,
    "participantes": [
      { "miembroId": 1, "montoAsignado": 33.34 },
      { "miembroId": 2, "montoAsignado": 33.33 },
      { "miembroId": 3, "montoAsignado": 33.33 }
    ]
  }
]
```

### `POST /grupos/{id}/gastos`
Registra un gasto y lo divide **en partes iguales** entre los participantes. Si el monto
no es divisible exactamente, los primeros participantes de la lista absorben el céntimo
extra (100.00 / 3 → 33.34, 33.33, 33.33), de modo que la suma siempre coincide con el monto.

| Campo | Tipo | Obligatorio | Notas |
|---|---|---|---|
| `descripcion` | string | sí | 1–200 caracteres |
| `monto` | number | sí | `> 0` |
| `pagadoPorId` | long | sí | Debe ser miembro del grupo |
| `categoria` | string | no | Por defecto `OTROS` |
| `participantes` | long[] | sí | Ids de miembros del grupo; mínimo 1; los duplicados se ignoran |
| `fecha` | long | no | Para gastos creados offline |

```json
{ "descripcion": "Cena en restaurante", "monto": 100, "pagadoPorId": 2, "categoria": "COMIDA", "participantes": [1, 2, 3] }
```
`201` → el gasto creado con su reparto. `400` por validación. `404` si el grupo no existe.

---

## Pagos (liquidación de deudas)

### `GET /grupos/{id}/pagos`
> Endpoint adicional al contrato original: la app lo necesita para sincronizar los pagos
> en Room y calcular los saldos offline.

`200` →
```json
[ { "id": 1, "grupoId": 1, "deudorId": 3, "acreedorId": 2, "monto": 10.5, "fecha": 1716508800000 } ]
```

### `POST /grupos/{id}/pagos`
Registra que `deudorId` le entregó `monto` a `acreedorId`.

```json
{ "deudorId": 3, "acreedorId": 2, "monto": 10.5 }
```
`201` → el pago creado. `400` si el monto es `<= 0`, deudor y acreedor son la misma
persona o alguno no pertenece al grupo. `404` si el grupo no existe.

---

## Cálculos

### `GET /grupos/{id}/saldos`
Balance neto de cada miembro (positivo = le deben, negativo = debe) y la lista de
deudas ya **simplificada** (algoritmo greedy portado de `BalanceCalculator.kt`; produce
como máximo N-1 transferencias y cancela deudas circulares).

`200` →
```json
{
  "balances": [
    { "miembroId": 1, "nombre": "Moisés", "balance": -20.01 },
    { "miembroId": 2, "nombre": "Ana", "balance": 49.5 },
    { "miembroId": 3, "nombre": "Luis", "balance": -29.49 }
  ],
  "deudas": [
    { "deudorId": 3, "deudorNombre": "Luis", "acreedorId": 2, "acreedorNombre": "Ana", "monto": 29.49 },
    { "deudorId": 1, "deudorNombre": "Moisés", "acreedorId": 2, "acreedorNombre": "Ana", "monto": 20.01 }
  ]
}
```

### `GET /grupos/{id}/estadisticas`
Gasto total por categoría, de mayor a menor. Las categorías sin gastos se omiten.
`porcentaje` va de 0 a 100 con un decimal.

`200` →
```json
{
  "total": 120.0,
  "categorias": [
    { "categoria": "COMIDA", "monto": 100.0, "porcentaje": 83.3 },
    { "categoria": "TRANSPORTE", "monto": 20.0, "porcentaje": 16.7 }
  ]
}
```

### `GET /grupos/{id}/actividad?limite=50`
Historial combinado de gastos y pagos, del más reciente al más antiguo.
`limite` es opcional (1–500, por defecto 50; fuera de rango → `400`).

`200` →
```json
[
  {
    "tipo": "PAGO", "id": 1, "fecha": 1716508900000, "monto": 10.5,
    "descripcion": "Luis le pagó a Ana",
    "miembroId": 3, "miembroNombre": "Luis", "acreedorId": 2, "acreedorNombre": "Ana"
  },
  {
    "tipo": "GASTO", "id": 2, "fecha": 1716508800000, "monto": 20.0,
    "descripcion": "Taxi al aeropuerto",
    "miembroId": 1, "miembroNombre": "Moisés", "categoria": "TRANSPORTE", "numeroParticipantes": 3
  }
]
```
Los campos que no aplican a un tipo (`acreedorId` en gastos, `categoria` en pagos) se omiten.

---

## Limitaciones conocidas (para fases siguientes)

- **Sin autenticación**: `esUsuarioActual` se guarda tal como lo envía el dispositivo que
  creó el grupo. Con login (Fase 3) pasará a derivarse del usuario autenticado.
- **Sin idempotencia en los POST**: si la respuesta de un POST se pierde por la red y el
  cliente reintenta, el recurso se duplica. La solución prevista es un encabezado
  `Idempotency-Key` (UUID generado por la app) con restricción `UNIQUE` en la base.
- **Sin edición ni borrado** de gastos o pagos todavía.
