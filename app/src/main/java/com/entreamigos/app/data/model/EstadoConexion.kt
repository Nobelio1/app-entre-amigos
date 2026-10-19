package com.entreamigos.app.data.model

data class EstadoConexion(
    val sincronizacion: Resource<Long> = Resource.Loading,
    val sinConexion: Boolean = false,
    val cambiosPendientes: Int = 0
) {
    val sincronizando: Boolean get() = sincronizacion is Resource.Loading

    val mensajeError: String?
        get() = (sincronizacion as? Resource.Error)
            ?.takeIf { it.tipo != TipoError.SIN_CONEXION }
            ?.mensaje
}
