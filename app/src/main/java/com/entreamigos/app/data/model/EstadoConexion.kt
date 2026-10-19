package com.entreamigos.app.data.model

/**
 * Lo que la UI necesita saber sobre la relación con el backend.
 *
 * @property sincronizacion `Loading` mientras se sincroniza, `Success(epochMillis)` con
 *   la hora de la última sincronización correcta, o `Error` con el motivo.
 * @property sinConexion true si el dispositivo no tiene red o el backend no respondió:
 *   la app sigue funcionando con los datos locales (modo offline).
 * @property cambiosPendientes registros creados localmente que aún no llegaron al servidor.
 */
data class EstadoConexion(
    val sincronizacion: Resource<Long> = Resource.Loading,
    val sinConexion: Boolean = false,
    val cambiosPendientes: Int = 0
) {
    val sincronizando: Boolean get() = sincronizacion is Resource.Loading

    /** Mensaje de un error "real" (4xx/5xx). La falta de conexión se muestra aparte. */
    val mensajeError: String?
        get() = (sincronizacion as? Resource.Error)
            ?.takeIf { it.tipo != TipoError.SIN_CONEXION }
            ?.mensaje
}
