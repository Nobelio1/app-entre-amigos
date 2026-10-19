package com.entreamigos.app.data.repository

import android.util.Log
import androidx.room.withTransaction
import com.entreamigos.app.data.local.AppDatabase
import com.entreamigos.app.data.local.entity.CategoriaGasto
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
 * Motor de sincronización offline-first entre Room y el backend.
 *
 * Reglas:
 * 1. **Room es la única fuente que lee la UI.** Esta clase nunca entrega datos remotos
 *    directamente: los escribe en Room y los Flow de los DAOs hacen el resto.
 * 2. **Escritura local primero.** Los repositorios guardan en Room (`sincronizado = false`)
 *    y luego llaman a [subirPendientesEnSegundoPlano] (write-through). Si no hay red, el
 *    registro queda pendiente y se reintenta en la próxima sincronización.
 * 3. **Subida antes que bajada.** [sincronizar] primero sube lo pendiente y después
 *    descarga todo y hace upsert por `remoteId`; así lo recién creado offline no se
 *    pierde ni se duplica.
 * 4. **Un solo proceso a la vez.** Un [Mutex] serializa subidas y bajadas para que un
 *    pull-to-refresh y un write-through simultáneos no suban dos veces lo mismo.
 *
 * Los ids locales nunca cambian (las FK de Room siguen siendo válidas); la traducción
 * local ↔ remoto se hace con la columna `remoteId` de cada entidad.
 */
class SincronizacionRepository(
    private val database: AppDatabase,
    private val remoto: RemoteDataSource,
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
        // Cuando la red vuelve, sube lo pendiente y refresca sin esperar al usuario.
        scope.launch {
            conectividad.hayRed.drop(1).filter { it }.collect { sincronizar() }
        }
    }

    fun sincronizarEnSegundoPlano() {
        scope.launch { sincronizar() }
    }

    /** Write-through: se llama después de cada escritura local. */
    fun subirPendientesEnSegundoPlano() {
        scope.launch {
            mutex.withLock {
                val resultado = subirPendientes()
                _estado.value = when (resultado) {
                    is Resource.Success -> Resource.Success(System.currentTimeMillis())
                    else -> resultado.comoEstado()
                }
            }
        }
    }

    /** Sincronización completa (subida + bajada). Devuelve y publica el resultado. */
    suspend fun sincronizar(): Resource<Long> = mutex.withLock {
        _estado.value = Resource.Loading
        val resultado = when (val subida = subirPendientes()) {
            is Resource.Success -> descargarTodo()
            else -> subida
        }
        val estado = when (resultado) {
            is Resource.Success -> Resource.Success(System.currentTimeMillis())
            else -> resultado.comoEstado()
        }
        _estado.value = estado
        estado
    }

    // ------------------------------------------------------------------ Subida

    /**
     * Sube grupos → miembros → gastos → pagos, en ese orden, porque cada nivel necesita
     * el `remoteId` del anterior. Un error de conexión o 5xx corta la subida (no tiene
     * sentido seguir); un 4xx solo descarta ese registro de esta ronda para que un dato
     * inválido no bloquee la cola entera, y se informa al final.
     */
    private suspend fun subirPendientes(): Resource<Unit> {
        var errorDeDatos: Resource.Error? = null
        fun registrar(error: Resource.Error, descripcion: String): Resource.Error? {
            Log.w(TAG, "No se pudo subir $descripcion: ${error.mensaje}")
            if (error.tipo != TipoError.SOLICITUD_INVALIDA) return error
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
            when (val r = remoto.llamar { crearGrupo(request) }) {
                is Resource.Success -> database.withTransaction {
                    grupoDao.marcarSincronizado(grupo.id, r.data.id)
                    // El backend devuelve los miembros en el mismo orden en que se enviaron
                    r.data.miembros.zip(miembros).forEach { (remoto, local) ->
                        miembroDao.marcarSincronizado(local.id, remoto.id)
                    }
                }
                is Resource.Error -> registrar(r, "el grupo ${grupo.id}")?.let { return it }
                Resource.Loading -> Unit
            }
        }

        // Miembros agregados después de que su grupo ya existía en el servidor
        for (miembro in miembroDao.obtenerPendientes()) {
            val grupoRemoteId = grupoDao.obtener(miembro.grupoId)?.remoteId ?: continue
            val request = CrearMiembroRequest(miembro.nombre, miembro.esUsuarioActual, miembro.colorAvatarHex)
            when (val r = remoto.llamar { agregarMiembro(grupoRemoteId, request) }) {
                is Resource.Success -> miembroDao.marcarSincronizado(miembro.id, r.data.id)
                is Resource.Error -> registrar(r, "el miembro ${miembro.id}")?.let { return it }
                Resource.Loading -> Unit
            }
        }

        for (gasto in gastoDao.obtenerPendientes()) {
            val grupoRemoteId = grupoDao.obtener(gasto.grupoId)?.remoteId ?: continue
            val remotoPorLocal = miembroDao.obtenerMiembros(gasto.grupoId)
                .associate { it.id to it.remoteId }
            val pagadoPor = remotoPorLocal[gasto.pagadoPorId] ?: continue
            // Mismo orden que al dividir localmente: el servidor asigna el céntimo
            // sobrante a los primeros de la lista, igual que BalanceCalculator.
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
            when (val r = remoto.llamar { registrarGasto(grupoRemoteId, request) }) {
                is Resource.Success -> database.withTransaction {
                    gastoDao.marcarSincronizado(gasto.id, r.data.id)
                    val localPorRemoto = remotoPorLocal.entries
                        .mapNotNull { (local, remoto) -> remoto?.let { it to local } }
                        .toMap()
                    reemplazarParticipantes(gasto.id, r.data, localPorRemoto)
                }
                is Resource.Error -> registrar(r, "el gasto ${gasto.id}")?.let { return it }
                Resource.Loading -> Unit
            }
        }

        for (pago in pagoDao.obtenerPendientes()) {
            val grupoRemoteId = grupoDao.obtener(pago.grupoId)?.remoteId ?: continue
            val remotoPorLocal = miembroDao.obtenerMiembros(pago.grupoId).associate { it.id to it.remoteId }
            val deudor = remotoPorLocal[pago.deudorId] ?: continue
            val acreedor = remotoPorLocal[pago.acreedorId] ?: continue
            val request = CrearPagoRequest(deudor, acreedor, pago.monto, pago.fecha)
            when (val r = remoto.llamar { registrarPago(grupoRemoteId, request) }) {
                is Resource.Success -> pagoDao.marcarSincronizado(pago.id, r.data.id)
                is Resource.Error -> registrar(r, "el pago ${pago.id}")?.let { return it }
                Resource.Loading -> Unit
            }
        }

        return errorDeDatos ?: Resource.Success(Unit)
    }

    // ------------------------------------------------------------------ Bajada

    /**
     * Trae todos los grupos y, por cada uno, sus gastos y pagos, y hace upsert en Room.
     * El servidor gana sobre los registros ya sincronizados; los pendientes locales
     * (sin `remoteId`) no se tocan.
     */
    private suspend fun descargarTodo(): Resource<Unit> {
        val grupos = when (val r = remoto.llamar { listarGrupos() }) {
            is Resource.Success -> r.data
            is Resource.Error -> return r
            Resource.Loading -> return Resource.Loading
        }

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

            // Una transacción por grupo: la UI nunca ve un grupo a medio importar
            database.withTransaction {
                val grupoId = upsertGrupo(grupoDto)
                val localPorRemoto = grupoDto.miembros.associate { it.id to upsertMiembro(it, grupoId) }
                gastos.forEach { upsertGasto(it, grupoId, localPorRemoto) }
                pagos.forEach { upsertPago(it, grupoId, localPorRemoto) }
            }
        }
        return Resource.Success(Unit)
    }

    private suspend fun upsertGrupo(dto: GrupoDto): Long {
        val existente = grupoDao.buscarPorRemoteId(dto.id)
        val entidad = GrupoEntity(
            id = existente?.id ?: 0L,
            nombre = dto.nombre,
            icono = GrupoIcono.desdeNombre(dto.icono),
            colorHex = dto.colorHex,
            fechaCreacion = dto.fechaCreacion,
            remoteId = dto.id,
            sincronizado = true
        )
        return if (existente == null) grupoDao.insertar(entidad) else entidad.id.also { grupoDao.actualizar(entidad) }
    }

    private suspend fun upsertMiembro(dto: MiembroDto, grupoIdLocal: Long): Long {
        val existente = miembroDao.buscarPorRemoteId(dto.id)
        val entidad = MiembroEntity(
            id = existente?.id ?: 0L,
            grupoId = grupoIdLocal,
            nombre = dto.nombre,
            esUsuarioActual = dto.esUsuarioActual,
            colorAvatarHex = dto.colorAvatarHex,
            remoteId = dto.id,
            sincronizado = true
        )
        return if (existente == null) miembroDao.insertar(entidad) else entidad.id.also { miembroDao.actualizar(entidad) }
    }

    private suspend fun upsertGasto(dto: GastoDto, grupoIdLocal: Long, localPorRemoto: Map<Long, Long>) {
        val pagadoPor = localPorRemoto[dto.pagadoPorId] ?: return
        val existente = gastoDao.buscarPorRemoteId(dto.id)
        val entidad = GastoEntity(
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
        val gastoId = if (existente == null) gastoDao.insertarGasto(entidad) else entidad.id.also { gastoDao.actualizarGasto(entidad) }
        reemplazarParticipantes(gastoId, dto, localPorRemoto)
    }

    private suspend fun upsertPago(dto: PagoDto, grupoIdLocal: Long, localPorRemoto: Map<Long, Long>) {
        val deudor = localPorRemoto[dto.deudorId] ?: return
        val acreedor = localPorRemoto[dto.acreedorId] ?: return
        val existente = pagoDao.buscarPorRemoteId(dto.id)
        val entidad = PagoEntity(
            id = existente?.id ?: 0L,
            grupoId = grupoIdLocal,
            deudorId = deudor,
            acreedorId = acreedor,
            monto = dto.monto,
            fecha = dto.fecha,
            remoteId = dto.id,
            sincronizado = true
        )
        if (existente == null) pagoDao.insertar(entidad) else pagoDao.actualizar(entidad)
    }

    /** El reparto del servidor es el definitivo (mismo algoritmo, pero manda el backend). */
    private suspend fun reemplazarParticipantes(gastoIdLocal: Long, dto: GastoDto, localPorRemoto: Map<Long, Long>) {
        val participantes = dto.participantes.mapNotNull { p ->
            localPorRemoto[p.miembroId]?.let { GastoParticipanteEntity(gastoId = gastoIdLocal, miembroId = it, montoAsignado = p.montoAsignado) }
        }
        if (participantes.size != dto.participantes.size) return
        gastoDao.eliminarParticipantes(gastoIdLocal)
        gastoDao.insertarParticipantes(participantes)
    }

    private fun Resource<Unit>.comoEstado(): Resource<Long> = when (this) {
        is Resource.Error -> this
        else -> Resource.Error("Sincronización incompleta")
    }

    private companion object {
        const val TAG = "Sincronizacion"
    }
}
