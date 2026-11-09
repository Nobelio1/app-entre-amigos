package com.entreamigos.backend.service

import com.entreamigos.backend.db.DatabaseFactory.consulta
import com.entreamigos.backend.db.GastoParticipantes
import com.entreamigos.backend.db.Gastos
import com.entreamigos.backend.db.Pagos
import com.entreamigos.backend.domain.BalanceCalculator
import com.entreamigos.backend.domain.GastoCalculo
import com.entreamigos.backend.domain.PagoCalculo
import com.entreamigos.backend.domain.ParticipacionCalculo
import com.entreamigos.backend.domain.aBigDecimal
import com.entreamigos.backend.domain.aCentavos
import com.entreamigos.backend.domain.aSoles
import com.entreamigos.backend.model.ActividadDto
import com.entreamigos.backend.model.BalanceMiembroDto
import com.entreamigos.backend.model.CategoriaGasto
import com.entreamigos.backend.model.CrearGastoRequest
import com.entreamigos.backend.model.CrearPagoRequest
import com.entreamigos.backend.model.DeudaDto
import com.entreamigos.backend.model.EstadisticaCategoriaDto
import com.entreamigos.backend.model.EstadisticasDto
import com.entreamigos.backend.model.GastoDto
import com.entreamigos.backend.model.MiembroDto
import com.entreamigos.backend.model.PagoDto
import com.entreamigos.backend.model.SaldosDto
import com.entreamigos.backend.model.ParticipanteDto
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

class GastoService(private val grupoService: GrupoService) {

    suspend fun listarGastos(grupoId: Long, usuarioId: Long): List<GastoDto> = consulta {
        miembrosOFallar(grupoId, usuarioId)
        gastosDe(grupoId)
    }

    suspend fun registrarGasto(
        grupoId: Long,
        request: CrearGastoRequest,
        usuarioId: Long,
        clave: String?
    ): Creado<GastoDto> {
        val datos = request.validado()
        consulta { miembrosOFallar(grupoId, usuarioId) }
        return crearIdempotente(
            clave = clave,
            buscar = { k ->
                val fila = Gastos.selectAll().where { Gastos.idempotencyKey eq k }.singleOrNull()
                    ?: return@crearIdempotente null
                if (fila[Gastos.grupoId] != grupoId) throw ConflictoException(GrupoService.CLAVE_REUTILIZADA)
                gastosDe(grupoId, soloGastoId = fila[Gastos.id]).single()
            },
            crear = {
                datos.validarMiembros(miembrosOFallar(grupoId, usuarioId))
                val gastoId = Gastos.insert {
                    it[Gastos.grupoId] = grupoId
                    it[descripcion] = datos.descripcion
                    it[monto] = datos.montoCentavos.aBigDecimal()
                    it[pagadoPorId] = datos.pagadoPorId
                    it[categoria] = datos.categoria.name
                    it[fecha] = datos.fecha ?: System.currentTimeMillis()
                    it[idempotencyKey] = clave
                } get Gastos.id
                insertarReparto(gastoId, datos)
                gastosDe(grupoId, soloGastoId = gastoId).single()
            }
        )
    }

    /** Reemplaza todos los datos del gasto y recalcula el reparto en partes iguales. */
    suspend fun actualizarGasto(grupoId: Long, gastoId: Long, request: CrearGastoRequest, usuarioId: Long): GastoDto {
        val datos = request.validado()
        return consulta {
            datos.validarMiembros(miembrosOFallar(grupoId, usuarioId))
            val filas = Gastos.update({ (Gastos.id eq gastoId) and (Gastos.grupoId eq grupoId) }) {
                it[descripcion] = datos.descripcion
                it[monto] = datos.montoCentavos.aBigDecimal()
                it[pagadoPorId] = datos.pagadoPorId
                it[categoria] = datos.categoria.name
                if (datos.fecha != null) it[fecha] = datos.fecha
                it[fechaActualizacion] = System.currentTimeMillis()
            }
            if (filas == 0) throw RecursoNoEncontradoException("Gasto $gastoId no encontrado en el grupo $grupoId")
            GastoParticipantes.deleteWhere { GastoParticipantes.gastoId eq gastoId }
            insertarReparto(gastoId, datos)
            gastosDe(grupoId, soloGastoId = gastoId).single()
        }
    }

    suspend fun eliminarGasto(grupoId: Long, gastoId: Long, usuarioId: Long) = consulta {
        miembrosOFallar(grupoId, usuarioId)
        val filas = Gastos.deleteWhere { (Gastos.id eq gastoId) and (Gastos.grupoId eq grupoId) }
        if (filas == 0) throw RecursoNoEncontradoException("Gasto $gastoId no encontrado en el grupo $grupoId")
    }

    suspend fun listarPagos(grupoId: Long, usuarioId: Long): List<PagoDto> = consulta {
        miembrosOFallar(grupoId, usuarioId)
        pagosDe(grupoId)
    }

    suspend fun registrarPago(
        grupoId: Long,
        request: CrearPagoRequest,
        usuarioId: Long,
        clave: String?
    ): Creado<PagoDto> {
        validar(request.monto > 0.0 && request.monto.isFinite()) { "El monto debe ser mayor a cero" }
        validar(request.monto <= MONTO_MAXIMO) { "El monto supera el máximo permitido" }
        validar(request.deudorId != request.acreedorId) { "El deudor y el acreedor deben ser distintos" }
        consulta { miembrosOFallar(grupoId, usuarioId) }

        return crearIdempotente(
            clave = clave,
            buscar = { k ->
                val fila = Pagos.selectAll().where { Pagos.idempotencyKey eq k }.singleOrNull()
                    ?: return@crearIdempotente null
                if (fila[Pagos.grupoId] != grupoId) throw ConflictoException(GrupoService.CLAVE_REUTILIZADA)
                fila.aPagoDto()
            },
            crear = {
                val idsMiembros = miembrosOFallar(grupoId, usuarioId).map { it.id }.toSet()
                validar(request.deudorId in idsMiembros) { "deudorId no pertenece al grupo" }
                validar(request.acreedorId in idsMiembros) { "acreedorId no pertenece al grupo" }

                val pagoId = Pagos.insert {
                    it[Pagos.grupoId] = grupoId
                    it[deudorId] = request.deudorId
                    it[acreedorId] = request.acreedorId
                    it[monto] = request.monto.aCentavos().aBigDecimal()
                    it[fecha] = request.fecha ?: System.currentTimeMillis()
                    it[idempotencyKey] = clave
                } get Pagos.id
                Pagos.selectAll().where { Pagos.id eq pagoId }.single().aPagoDto()
            }
        )
    }

    suspend fun calcularSaldos(grupoId: Long, usuarioId: Long): SaldosDto = consulta {
        val miembros = miembrosOFallar(grupoId, usuarioId)
        val gastos = gastosDe(grupoId)
        val pagos = pagosDe(grupoId)

        val balances = BalanceCalculator.calcularBalancesNetos(
            miembroIds = miembros.map { it.id },
            gastos = gastos.map { GastoCalculo(it.pagadoPorId, it.monto.aCentavos()) },
            participaciones = gastos.flatMap { gasto ->
                gasto.participantes.map { ParticipacionCalculo(it.miembroId, it.montoAsignado.aCentavos()) }
            },
            pagos = pagos.map { PagoCalculo(it.deudorId, it.acreedorId, it.monto.aCentavos()) }
        )
        val nombres = miembros.associate { it.id to it.nombre }

        SaldosDto(
            balances = miembros.map { BalanceMiembroDto(it.id, it.nombre, (balances[it.id] ?: 0L).aSoles()) },
            deudas = BalanceCalculator.simplificarDeudas(balances).map {
                DeudaDto(
                    deudorId = it.deudorId,
                    deudorNombre = nombres[it.deudorId].orEmpty(),
                    acreedorId = it.acreedorId,
                    acreedorNombre = nombres[it.acreedorId].orEmpty(),
                    monto = it.montoCentavos.aSoles()
                )
            }
        )
    }

    suspend fun calcularEstadisticas(grupoId: Long, usuarioId: Long): EstadisticasDto = consulta {
        miembrosOFallar(grupoId, usuarioId)
        val centavosPorCategoria = Gastos.selectAll().where { Gastos.grupoId eq grupoId }
            .groupBy({ it[Gastos.categoria] }, { it[Gastos.monto].aCentavos() })
            .mapValues { it.value.sum() }
        val total = centavosPorCategoria.values.sum()

        EstadisticasDto(
            total = total.aSoles(),
            categorias = centavosPorCategoria
                .filterValues { it > 0 }
                .entries
                .sortedByDescending { it.value }
                .map { (categoria, centavos) ->
                    EstadisticaCategoriaDto(
                        categoria = categoria,
                        monto = centavos.aSoles(),
                        porcentaje = Math.round(centavos * 1000.0 / total) / 10.0
                    )
                }
        )
    }

    suspend fun obtenerActividad(grupoId: Long, usuarioId: Long, limite: Int): List<ActividadDto> = consulta {
        val nombres = miembrosOFallar(grupoId, usuarioId).associate { it.id to it.nombre }
        val gastos = gastosDe(grupoId).map {
            ActividadDto(
                tipo = "GASTO",
                id = it.id,
                fecha = it.fecha,
                monto = it.monto,
                descripcion = it.descripcion,
                miembroId = it.pagadoPorId,
                miembroNombre = nombres[it.pagadoPorId].orEmpty(),
                categoria = it.categoria,
                numeroParticipantes = it.participantes.size
            )
        }
        val pagos = pagosDe(grupoId).map {
            val deudor = nombres[it.deudorId].orEmpty()
            val acreedor = nombres[it.acreedorId].orEmpty()
            ActividadDto(
                tipo = "PAGO",
                id = it.id,
                fecha = it.fecha,
                monto = it.monto,
                descripcion = "$deudor le pagó a $acreedor",
                miembroId = it.deudorId,
                miembroNombre = deudor,
                acreedorId = it.acreedorId,
                acreedorNombre = acreedor
            )
        }
        (gastos + pagos).sortedByDescending { it.fecha }.take(limite)
    }

    private fun miembrosOFallar(grupoId: Long, usuarioId: Long): List<MiembroDto> =
        grupoService.miembrosConAcceso(grupoId, usuarioId)

    private class DatosGasto(
        val descripcion: String,
        val montoCentavos: Long,
        val pagadoPorId: Long,
        val categoria: CategoriaGasto,
        val participantes: List<Long>,
        val fecha: Long?
    ) {
        fun validarMiembros(miembros: List<MiembroDto>) {
            val ids = miembros.map { it.id }.toSet()
            validar(pagadoPorId in ids) { "pagadoPorId no pertenece al grupo" }
            validar(ids.containsAll(participantes)) { "Hay participantes que no pertenecen al grupo" }
        }
    }

    private fun CrearGastoRequest.validado(): DatosGasto {
        val limpia = descripcion.trim()
        validar(limpia.isNotEmpty()) { "La descripción no puede estar vacía" }
        validar(limpia.length <= 200) { "La descripción no puede superar 200 caracteres" }
        validar(monto > 0.0 && monto.isFinite()) { "El monto debe ser mayor a cero" }
        validar(monto <= MONTO_MAXIMO) { "El monto supera el máximo permitido" }
        val distintos = participantes.distinct()
        validar(distintos.isNotEmpty()) { "Selecciona al menos un participante" }
        return DatosGasto(limpia, monto.aCentavos(), pagadoPorId, parsearCategoria(categoria), distintos, fecha)
    }

    private fun insertarReparto(gastoId: Long, datos: DatosGasto) {
        val reparto = BalanceCalculator.dividirEnPartesIguales(datos.montoCentavos, datos.participantes)
        GastoParticipantes.batchInsert(reparto.entries) { (miembroId, centavos) ->
            this[GastoParticipantes.gastoId] = gastoId
            this[GastoParticipantes.miembroId] = miembroId
            this[GastoParticipantes.montoAsignado] = centavos.aBigDecimal()
        }
    }

    private fun gastosDe(grupoId: Long, soloGastoId: Long? = null): List<GastoDto> {
        val filas = Gastos.selectAll()
            .where { Gastos.grupoId eq grupoId }
            .apply { if (soloGastoId != null) andWhere { Gastos.id eq soloGastoId } }
            .orderBy(Gastos.fecha, SortOrder.DESC)
            .toList()
        if (filas.isEmpty()) return emptyList()

        val participantesPorGasto = GastoParticipantes.selectAll()
            .where { GastoParticipantes.gastoId inList filas.map { it[Gastos.id] } }
            .orderBy(GastoParticipantes.id)
            .groupBy(
                { it[GastoParticipantes.gastoId] },
                { ParticipanteDto(it[GastoParticipantes.miembroId], it[GastoParticipantes.montoAsignado].toDouble()) }
            )

        return filas.map {
            GastoDto(
                id = it[Gastos.id],
                grupoId = it[Gastos.grupoId],
                descripcion = it[Gastos.descripcion],
                monto = it[Gastos.monto].toDouble(),
                pagadoPorId = it[Gastos.pagadoPorId],
                categoria = it[Gastos.categoria],
                fecha = it[Gastos.fecha],
                participantes = participantesPorGasto[it[Gastos.id]].orEmpty(),
                fechaActualizacion = it[Gastos.fechaActualizacion]
            )
        }
    }

    private fun pagosDe(grupoId: Long): List<PagoDto> =
        Pagos.selectAll().where { Pagos.grupoId eq grupoId }
            .orderBy(Pagos.fecha, SortOrder.DESC)
            .map { it.aPagoDto() }

    private fun ResultRow.aPagoDto() = PagoDto(
        id = this[Pagos.id],
        grupoId = this[Pagos.grupoId],
        deudorId = this[Pagos.deudorId],
        acreedorId = this[Pagos.acreedorId],
        monto = this[Pagos.monto].toDouble(),
        fecha = this[Pagos.fecha]
    )

    private fun parsearCategoria(valor: String): CategoriaGasto =
        CategoriaGasto.entries.firstOrNull { it.name.equals(valor.trim(), ignoreCase = true) }
            ?: throw SolicitudInvalidaException(
                "Categoría inválida '$valor'. Valores permitidos: ${CategoriaGasto.entries.joinToString()}"
            )

    companion object {
        private const val MONTO_MAXIMO = 9_999_999_999.99
    }
}
