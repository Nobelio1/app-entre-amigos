package com.entreamigos.app.data.local

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferenciasUsuario(context: Context) {

    private val prefs = context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)

    private val _ultimaVistaNotificaciones = MutableStateFlow(prefs.getLong(CLAVE_ULTIMA_VISTA, 0L))

    val ultimaVistaNotificaciones: StateFlow<Long> = _ultimaVistaNotificaciones.asStateFlow()

    fun marcarNotificacionesVistas(momento: Long = System.currentTimeMillis()) {
        prefs.edit().putLong(CLAVE_ULTIMA_VISTA, momento).apply()
        _ultimaVistaNotificaciones.value = momento
    }

    private companion object {
        const val ARCHIVO = "entreamigos_preferencias"
        const val CLAVE_ULTIMA_VISTA = "ultima_vista_notificaciones"
    }
}
