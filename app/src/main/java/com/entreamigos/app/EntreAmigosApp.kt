package com.entreamigos.app

import android.app.Application
import com.entreamigos.app.data.repository.AppContainer
import com.entreamigos.app.notification.NotificationHelper

class EntreAmigosApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.crearCanalNotificaciones(this)
        // Sincronización al abrir la app; si no hay backend la UI sigue con Room
        container.sincronizacionRepository.sincronizarEnSegundoPlano()
    }
}
