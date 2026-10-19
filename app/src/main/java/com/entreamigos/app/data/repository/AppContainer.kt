package com.entreamigos.app.data.repository

import android.content.Context
import com.entreamigos.app.BuildConfig
import com.entreamigos.app.data.local.AppDatabase
import com.entreamigos.app.data.local.PreferenciasUsuario
import com.entreamigos.app.data.remote.ConectividadMonitor
import com.entreamigos.app.data.remote.NetworkModule
import com.entreamigos.app.data.remote.RemoteDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(context: Context) {

    private val database = AppDatabase.getInstance(context)

    private val scopeAplicacion = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val remoteDataSource = RemoteDataSource(
        NetworkModule.crearApi(baseUrl = BuildConfig.API_BASE_URL, registrarTrafico = BuildConfig.DEBUG)
    )

    val sincronizacionRepository: SincronizacionRepository by lazy {
        SincronizacionRepository(
            database = database,
            remoto = remoteDataSource,
            conectividad = ConectividadMonitor(context.applicationContext),
            scope = scopeAplicacion
        )
    }

    val grupoRepository: GrupoRepository by lazy {
        GrupoRepository(
            database = database,
            grupoDao = database.grupoDao(),
            miembroDao = database.miembroDao(),
            gastoDao = database.gastoDao(),
            pagoDao = database.pagoDao(),
            sincronizacion = sincronizacionRepository
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
            preferencias = PreferenciasUsuario(context.applicationContext)
        )
    }
}
