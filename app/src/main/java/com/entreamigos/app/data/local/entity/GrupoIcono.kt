package com.entreamigos.app.data.local.entity

/**
 * Ícono de un grupo, persistido por su nombre (`VIAJE`, `CASA`, ...).
 *
 * Reemplaza al antiguo campo `emoji: String`: un emoji se dibuja distinto según el
 * fabricante y la versión de Android (o ni se dibuja), mientras que un identificador
 * cerrado se traduce siempre al mismo ImageVector en `ui/components/Iconografia.kt`.
 * Esta clase no depende de Compose para que la capa de datos siga siendo pura.
 *
 * El nombre es también el valor que viaja en el campo `icono` de la API REST.
 */
enum class GrupoIcono {
    VIAJE, CASA, SALIDAS, ESTUDIOS, AUTO, FIESTA, COMIDA, DEPORTE, GENERAL;

    companion object {
        /** Tolerante con valores desconocidos que pueda devolver el backend. */
        fun desdeNombre(valor: String?): GrupoIcono =
            entries.firstOrNull { it.name.equals(valor?.trim(), ignoreCase = true) } ?: GENERAL
    }
}
