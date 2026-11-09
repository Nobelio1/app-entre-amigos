package com.entreamigos.app.data.repository

import android.content.Context
import com.entreamigos.app.BuildConfig
import com.entreamigos.app.data.local.AppDatabase
import com.entreamigos.app.data.local.PreferenciasUsuario
import com.entreamigos.app.data.local.SesionManager
import com.entreamigos.app.data.remote.ConectividadMonitor
import com.entreamigos.app.data.remote.GoogleAuthCliente
import com.entreamigos.app.data.remote.NetworkModule
import com.entreamigos.app.data.remote.RemoteDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(context: Context) {

    private val database = AppDatabase.getInstance(context)

    private val scopeAplicacion = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val sesionManager = SesionManager(context.applicationContext)

    private val preferencias = PreferenciasUsuario(context.applicationContext)

    private val remoteDataSource = RemoteDataSource(
        NetworkModule.crearApi(
            baseUrl = BuildConfig.API_BASE_URL,
            registrarTrafico = BuildConfig.DEBUG,
            token = { sesionManager.token },
            // Token expirado o revocado: se vuelve a la pantalla de login (los datos locales se conservan)
            onNoAutorizado = { sesionManager.borrar() }
        )
    )

    val sincronizacionRepository: SincronizacionRepository by lazy {
        SincronizacionRepository(
            database = database,
            remoto = remoteDataSource,
            sesion = sesionManager,
            conectividad = ConectividadMonitor(context.applicationContext),
            scope = scopeAplicacion
        )
    }

    val authRepository: AuthRepository by lazy {
        AuthRepository(
            database = database,
            remoto = remoteDataSource,
            sesionManager = sesionManager,
            sincronizacion = sincronizacionRepository,
            preferencias = preferencias,
            googleAuth = GoogleAuthCliente(context.applicationContext)
        )
    }

    val grupoRepository: GrupoRepository by lazy {
        GrupoRepository(
            database = database,
            grupoDao = database.grupoDao(),
            miembroDao = database.miembroDao(),
            gastoDao = database.gastoDao(),
            pagoDao = database.pagoDao(),
            sincronizacion = sincronizacionRepository,
            sesion = sesionManager
        )
    }

    val gastoRepository: GastoRepository by lazy {
        GastoRepository(
            database = database,
            gastoDao = database.gastoDao(),
            miembroDao = database.miembroDao(),
            pagoDao = database.pagoDao(),
            grupoDao = database.grupoDao(),
            sincronizacion = sincronizacionRepository,
            preferencias = preferencias
        )
    }
}
