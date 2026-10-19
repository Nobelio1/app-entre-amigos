package com.entreamigos.app.data.local.entity

enum class GrupoIcono {
    VIAJE, CASA, SALIDAS, ESTUDIOS, AUTO, FIESTA, COMIDA, DEPORTE, GENERAL;

    companion object {
        fun desdeNombre(valor: String?): GrupoIcono =
            entries.firstOrNull { it.name.equals(valor?.trim(), ignoreCase = true) } ?: GENERAL
    }
}
