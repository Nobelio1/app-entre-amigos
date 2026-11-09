package com.entreamigos.app.data.repository

import android.util.Log
import androidx.room.withTransaction
import com.entreamigos.app.data.local.AppDatabase
import com.entreamigos.app.data.local.SesionManager
import com.entreamigos.app.data.local.entity.CategoriaGasto
import com.entreamigos.app.data.local.entity.GastoEliminadoEntity
import com.entreamigos.app.data.local.entity.GastoEntity
import com.entreamigos.app.data.local.entity.GastoParticipanteEntity
import com.entreamigos.app.data.local.entity.GrupoEntity
import com.entreamigos.app.data.local.entity.GrupoIcono
import com.entreamigos.app.data.local.entity.MiembroEntity
import com.entreamigos.app.data.local.entity.PagoEntity
import com.entreamigos.app.data.model.EstadoConexion
import com.entreamigos.app.data.model.Resource
import com.entreamigos.app.data.model.TipoError
import com.entreamigos.app.data.remote.ConectividadMonitor
import com.entreamigos.app.data.remote.RemoteDataSource
import com.entreamigos.app.data.remote.dto.CrearGastoRequest
import com.entreamigos.app.data.remote.dto.CrearGrupoRequest
import com.entreamigos.app.data.remote.dto.CrearMiembroRequest
import com.entreamigos.app.data.remote.dto.CrearPagoRequest
import com.entreamigos.app.data.remote.dto.GastoDto
import com.entreamigos.app.data.remote.dto.GrupoDto
import com.entreamigos.app.data.remote.dto.MiembroDto
import com.entreamigos.app.data.remote.dto.PagoDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Sincronización offline-first:
 *  1. **Subida** de lo pendiente en Room: altas (POST con `Idempotency-Key`), ediciones de
 *     gastos (PUT) y borrados de gastos (DELETE).
 *  2. **Descarga** de los grupos del usuario con upsert por `remoteId`, quitando lo que el
 *     servidor ya no devuelve (gastos borrados en otro dispositivo, grupos que dejó).
 */
class SincronizacionRepository(
    private val database: AppDatabase,
    private val remoto: RemoteDataSource,
    private val sesion: SesionManager,
    conectividad: ConectividadMonitor,
    private val scope: CoroutineScope
) {
    private val grupoDao = database.grupoDao()
    private val miembroDao = database.miembroDao()
    private val gastoDao = database.gastoDao()
    private val pagoDao = database.pagoDao()

    private val mutex = Mutex()

    private val _estado = MutableStateFlow<Resource<Long>>(Resource.Loading)
    val estado: StateFlow<Resource<Long>> = _estado.asStateFlow()

    val estadoConexion: Flow<EstadoConexion> = combine(
        _estado,
        conectividad.hayRed,
        pagoDao.observarCambiosPendientes()
    ) { estado, hayRed, pendientes ->
        EstadoConexion(
            sincronizacion = estado,
            sinConexion = !hayRed || (estado is Resource.Error && estado.tipo == TipoError.SIN_CONEXION),
            cambiosPendientes = pendientes
        )
    }.distinctUntilChanged()

    init {
        scope.launch {
            conectividad.hayRed.drop(1).filter { it }.collect { sincronizar() }
        }
    }

    fun sincronizarEnSegundoPlano() {
        scope.launch { sincronizar() }
    }

    fun subirPendientesEnSegundoPlano() {
        scope.launch {
            mutex.withLock {
                if (sesion.token == null) return@withLock
                _estado.value = when (val resultado = subirPendientes()) {
                    is Resource.Success -> Resource.Success(System.currentTimeMillis())
                    else -> resultado.comoEstado()
                }
            }
        }
    }

    suspend fun sincronizar(): Resource<Long> = mutex.withLock {
        if (sesion.token == null) return@withLock Resource.Error("Inicia sesión para sincronizar", TipoError.NO_AUTORIZADO)
        _estado.value = Resource.Loading
        val subida = subirPendientes()
        // Un registro rechazado por el servidor no debe bloquear la descarga del resto
        val resultado = if (subida is Resource.Success || subida.esErrorDeDatos()) {
            val descarga = descargarTodo()
            if (descarga is Resource.Success) subida else descarga
        } else {
            subida
        }
        val estado = when (resultado) {
            is Resource.Success -> Resource.Success(System.currentTimeMillis())
            else -> resultado.comoEstado()
        }
        _estado.value = estado
        estado
    }

    /** Ejecuta [bloque] sin que corra ninguna sincronización a la vez (p. ej. al cerrar sesión). */
    suspend fun <T> enExclusiva(bloque: suspend () -> T): T = mutex.withLock {
        val resultado = bloque()
        _estado.value = Resource.Loading
        resultado
    }

    private suspend fun subirPendientes(): Resource<Unit> {
        var errorDeDatos: Resource.Error? = null
        /** Devuelve el error si hay que abortar la subida (red, servidor o sesión). */
        fun registrar(error: Resource.Error, descripcion: String): Resource.Error? {
            Log.w(TAG, "No se pudo subir $descripcion: ${error.mensaje}")
            if (!error.esDeDatos()) return error
            errorDeDatos = error
            return null
        }

        for (grupo in grupoDao.obtenerPendientes()) {
            val miembros = miembroDao.obtenerMiembros(grupo.id)
            val request = CrearGrupoRequest(
                nombre = grupo.nombre,
                icono = grupo.icono.name,
                colorHex = grupo.colorHex,
                miembros = miembros.map { CrearMiembroRequest(it.nombre, it.esUsuarioActual, it.colorAvatarHex) },
                fechaCreacion = grupo.fechaCreacion
            )
            when (val r = remoto.llamar { crearGrupo(request, grupo.idempotencyKey) }) {
                is Resource.Success -> database.withTransaction {
                    grupoDao.marcarSincronizado(grupo.id, r.data.id, r.data.codigoInvitacion)
                    r.data.miembros.zip(miembros).forEach { (remoto, local) ->
                        miembroDao.marcarSincronizado(local.id, remoto.id, remoto.usuarioId)
                    }
                }
                is Resource.Error -> registrar(r, "el grupo ${grupo.id}")?.let { return it }
                Resource.Loading -> Unit
            }
        }

        for (miembro in miembroDao.obtenerPendientes()) {
            val grupoRemoteId = grupoDao.obtener(miembro.grupoId)?.remoteId ?: continue
            val request = CrearMiembroRequest(miembro.nombre, false, miembro.colorAvatarHex)
            when (val r = remoto.llamar { agregarMiembro(grupoRemoteId, request, miembro.idempotencyKey) }) {
                is Resource.Success -> miembroDao.marcarSincronizado(miembro.id, r.data.id, r.data.usuarioId)
                is Resource.Error -> registrar(r, "el miembro ${miembro.id}")?.let { return it }
                Resource.Loading -> Unit
            }
        }

        for (eliminacion in gastoDao.obtenerEliminacionesPendientes()) {
            when (val r = remoto.llamar { eliminarGasto(eliminacion.grupoRemoteId, eliminacion.remoteId) }) {
                is Resource.Success -> gastoDao.confirmarEliminacion(eliminacion.remoteId)
                // Ya no existe en el servidor: el objetivo del borrado se cumplió
                is Resource.Error -> if (r.tipo == TipoError.NO_ENCONTRADO) {
                    gastoDao.confirmarEliminacion(eliminacion.remoteId)
                } else {
                    registrar(r, "el borrado del gasto remoto ${eliminacion.remoteId}")?.let { return it }
                }
                Resource.Loading -> Unit
            }
        }

        for (gasto in gastoDao.obtenerPendientes()) {
            val grupoRemoteId = grupoDao.obtener(gasto.grupoId)?.remoteId ?: continue
            val remotoPorLocal = miembroDao.obtenerMiembros(gasto.grupoId)
                .associate { it.id to it.remoteId }
            val pagadoPor = remotoPorLocal[gasto.pagadoPorId] ?: continue
            val participantes = gastoDao.obtenerParticipantes(gasto.id).map { remotoPorLocal[it.miembroId] }
            if (participantes.isEmpty() || participantes.any { it == null }) continue

            val request = CrearGastoRequest(
                descripcion = gasto.descripcion,
                monto = gasto.monto,
                pagadoPorId = pagadoPor,
                categoria = gasto.categoria.name,
                participantes = participantes.filterNotNull(),
                fecha = gasto.fecha
            )
            val remoteId = gasto.remoteId
            val r = if (remoteId == null) {
                remoto.llamar { registrarGasto(grupoRemoteId, request, gasto.idempotencyKey) }
            } else {
                remoto.llamar { actualizarGasto(grupoRemoteId, remoteId, request) }
            }
            when (r) {
                is Resource.Success -> database.withTransaction {
                    val actual = gastoDao.obtener(gasto.id)
                    when {
                        // Se borró localmente mientras se subía: propagar el borrado
                        actual == null -> gastoDao.registrarEliminacion(GastoEliminadoEntity(r.data.id, grupoRemoteId))
                        // Se volvió a editar mientras se subía: queda pendiente para un PUT
                        actual.version != gasto.version -> gastoDao.asignarRemoteId(gasto.id, r.data.id)
                        else -> {
                            gastoDao.marcarSincronizado(gasto.id, r.data.id)
                            val localPorRemoto = remotoPorLocal.entries
                                .mapNotNull { (local, remoto) -> remoto?.let { it to local } }
                                .toMap()
                            reemplazarParticipantes(gasto.id, r.data, localPorRemoto)
                        }
                    }
                }
                is Resource.Error -> if (remoteId != null && r.tipo == TipoError.NO_ENCONTRADO) {
                    // Se editó aquí un gasto que otro integrante ya borró: gana el borrado
                    gastoDao.eliminarGasto(gasto)
                } else {
                    registrar(r, "el gasto ${gasto.id}")?.let { return it }
                }
                Resource.Loading -> Unit
            }
        }

        for (pago in pagoDao.obtenerPendientes()) {
            val grupoRemoteId = grupoDao.obtener(pago.grupoId)?.remoteId ?: continue
            val remotoPorLocal = miembroDao.obtenerMiembros(pago.grupoId).associate { it.id to it.remoteId }
            val deudor = remotoPorLocal[pago.deudorId] ?: continue
            val acreedor = remotoPorLocal[pago.acreedorId] ?: continue
            val request = CrearPagoRequest(deudor, acreedor, pago.monto, pago.fecha)
            when (val r = remoto.llamar { registrarPago(grupoRemoteId, request, pago.idempotencyKey) }) {
                is Resource.Success -> pagoDao.marcarSincronizado(pago.id, r.data.id)
                is Resource.Error -> registrar(r, "el pago ${pago.id}")?.let { return it }
                Resource.Loading -> Unit
            }
        }

        return errorDeDatos ?: Resource.Success(Unit)
    }

    private suspend fun descargarTodo(): Resource<Unit> {
        val grupos = when (val r = remoto.llamar { listarGrupos() }) {
            is Resource.Success -> r.data
            is Resource.Error -> return r
            Resource.Loading -> return Resource.Loading
        }

        // Grupos que el usuario ya no tiene en el servidor
        if (grupos.isEmpty()) grupoDao.eliminarTodosLosSincronizados()
        else grupoDao.eliminarAusentes(grupos.map { it.id })

        for (grupoDto in grupos) {
            val gastos = when (val r = remoto.llamar { listarGastos(grupoDto.id) }) {
                is Resource.Success -> r.data
                is Resource.Error -> return r
                Resource.Loading -> return Resource.Loading
            }
            val pagos = when (val r = remoto.llamar { listarPagos(grupoDto.id) }) {
                is Resource.Success -> r.data
                is Resource.Error -> return r
                Resource.Loading -> return Resource.Loading
            }

            database.withTransaction {
                val grupoId = upsertGrupo(grupoDto)
                val localPorRemoto = grupoDto.miembros.associate { it.id to upsertMiembro(it, grupoId) }
                val borradosPendientes = gastoDao.obtenerRemoteIdsEliminados().toSet()
                gastos.filter { it.id !in borradosPendientes }.forEach { upsertGasto(it, grupoId, localPorRemoto) }
                if (gastos.isEmpty()) gastoDao.eliminarSincronizadosDeGrupo(grupoId)
                else gastoDao.eliminarAusentes(grupoId, gastos.map { it.id })
                pagos.forEach { upsertPago(it, grupoId, localPorRemoto) }
            }
        }
        return Resource.Success(Unit)
    }

    private suspend fun upsertGrupo(dto: GrupoDto): Long {
        val existente = grupoDao.buscarPorRemoteId(dto.id)
        val base = GrupoEntity(
            id = existente?.id ?: 0L,
            nombre = dto.nombre,
            icono = GrupoIcono.desdeNombre(dto.icono),
            colorHex = dto.colorHex,
            fechaCreacion = dto.fechaCreacion,
            remoteId = dto.id,
            sincronizado = true,
            codigoInvitacion = dto.codigoInvitacion ?: existente?.codigoInvitacion
        )
        val entidad = existente?.let { base.copy(idempotencyKey = it.idempotencyKey) } ?: base
        return if (existente == null) grupoDao.insertar(entidad) else entidad.id.also { grupoDao.actualizar(entidad) }
    }

    private suspend fun upsertMiembro(dto: MiembroDto, grupoIdLocal: Long): Long {
        val existente = miembroDao.buscarPorRemoteId(dto.id)
        val base = MiembroEntity(
            id = existente?.id ?: 0L,
            grupoId = grupoIdLocal,
            nombre = dto.nombre,
            esUsuarioActual = dto.esUsuarioActual,
            colorAvatarHex = dto.colorAvatarHex,
            remoteId = dto.id,
            sincronizado = true,
            usuarioId = dto.usuarioId
        )
        val entidad = existente?.let { base.copy(idempotencyKey = it.idempotencyKey) } ?: base
        return if (existente == null) miembroDao.insertar(entidad) else entidad.id.also { miembroDao.actualizar(entidad) }
    }

    private suspend fun upsertGasto(dto: GastoDto, grupoIdLocal: Long, localPorRemoto: Map<Long, Long>) {
        val pagadoPor = localPorRemoto[dto.pagadoPorId] ?: return
        val existente = gastoDao.buscarPorRemoteId(dto.id)
        // Hay una edición local sin subir: no la pisamos con la versión del servidor
        if (existente != null && !existente.sincronizado) return
        val base = GastoEntity(
            id = existente?.id ?: 0L,
            grupoId = grupoIdLocal,
            descripcion = dto.descripcion,
            monto = dto.monto,
            pagadoPorId = pagadoPor,
            categoria = CategoriaGasto.entries.firstOrNull { it.name == dto.categoria } ?: CategoriaGasto.OTROS,
            fecha = dto.fecha,
            remoteId = dto.id,
            sincronizado = true
        )
        val entidad = existente?.let { base.copy(idempotencyKey = it.idempotencyKey, version = it.version) } ?: base
        val gastoId = if (existente == null) gastoDao.insertarGasto(entidad) else entidad.id.also { gastoDao.actualizarGasto(entidad) }
        reemplazarParticipantes(gastoId, dto, localPorRemoto)
    }

    private suspend fun upsertPago(dto: PagoDto, grupoIdLocal: Long, localPorRemoto: Map<Long, Long>) {
        val deudor = localPorRemoto[dto.deudorId] ?: return
        val acreedor = localPorRemoto[dto.acreedorId] ?: return
        val existente = pagoDao.buscarPorRemoteId(dto.id)
        val base = PagoEntity(
            id = existente?.id ?: 0L,
            grupoId = grupoIdLocal,
            deudorId = deudor,
            acreedorId = acreedor,
            monto = dto.monto,
            fecha = dto.fecha,
            remoteId = dto.id,
            sincronizado = true
        )
        val entidad = existente?.let { base.copy(idempotencyKey = it.idempotencyKey) } ?: base
        if (existente == null) pagoDao.insertar(entidad) else pagoDao.actualizar(entidad)
    }

    private suspend fun reemplazarParticipantes(gastoIdLocal: Long, dto: GastoDto, localPorRemoto: Map<Long, Long>) {
        val participantes = dto.participantes.mapNotNull { p ->
            localPorRemoto[p.miembroId]?.let { GastoParticipanteEntity(gastoId = gastoIdLocal, miembroId = it, montoAsignado = p.montoAsignado) }
        }
        if (participantes.size != dto.participantes.size) return
        gastoDao.eliminarParticipantes(gastoIdLocal)
        gastoDao.insertarParticipantes(participantes)
    }

    /** Errores atribuibles a un registro concreto (no a la red ni a la sesión). */
    private fun Resource.Error.esDeDatos() =
        tipo == TipoError.SOLICITUD_INVALIDA || tipo == TipoError.NO_ENCONTRADO

    private fun Resource<Unit>.esErrorDeDatos() = this is Resource.Error && esDeDatos()

    private fun Resource<Unit>.comoEstado(): Resource<Long> = when (this) {
        is Resource.Error -> this
        else -> Resource.Error("Sincronización incompleta")
    }

    private companion object {
        const val TAG = "Sincronizacion"
    }
}
