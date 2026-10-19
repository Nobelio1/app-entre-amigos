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
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll

class GastoService(private val grupoService: GrupoService) {

    suspend fun listarGastos(grupoId: Long): List<GastoDto> = consulta {
        miembrosOFallar(grupoId)
        gastosDe(grupoId)
    }

    suspend fun registrarGasto(grupoId: Long, request: CrearGastoRequest): GastoDto {
        val descripcion = request.descripcion.trim()
        validar(descripcion.isNotEmpty()) { "La descripción no puede estar vacía" }
        validar(descripcion.length <= 200) { "La descripción no puede superar 200 caracteres" }
        validar(request.monto > 0.0 && request.monto.isFinite()) { "El monto debe ser mayor a cero" }
        validar(request.monto <= MONTO_MAXIMO) { "El monto supera el máximo permitido" }
        val categoria = parsearCategoria(request.categoria)
        val participantes = request.participantes.distinct()
        validar(participantes.isNotEmpty()) { "Selecciona al menos un participante" }
        val montoCentavos = request.monto.aCentavos()

        return consulta {
            val idsMiembros = miembrosOFallar(grupoId).map { it.id }.toSet()
            validar(request.pagadoPorId in idsMiembros) { "pagadoPorId no pertenece al grupo" }
            validar(idsMiembros.containsAll(participantes)) { "Hay participantes que no pertenecen al grupo" }

            val gastoId = Gastos.insert {
                it[Gastos.grupoId] = grupoId
                it[Gastos.descripcion] = descripcion
                it[monto] = montoCentavos.aBigDecimal()
                it[pagadoPorId] = request.pagadoPorId
                it[Gastos.categoria] = categoria.name
                it[fecha] = request.fecha ?: System.currentTimeMillis()
            } get Gastos.id

            val reparto = BalanceCalculator.dividirEnPartesIguales(montoCentavos, participantes)
            GastoParticipantes.batchInsert(reparto.entries) { (miembroId, centavos) ->
                this[GastoParticipantes.gastoId] = gastoId
                this[GastoParticipantes.miembroId] = miembroId
                this[GastoParticipantes.montoAsignado] = centavos.aBigDecimal()
            }
            gastosDe(grupoId, soloGastoId = gastoId).single()
        }
    }

    suspend fun listarPagos(grupoId: Long): List<PagoDto> = consulta {
        miembrosOFallar(grupoId)
        pagosDe(grupoId)
    }

    suspend fun registrarPago(grupoId: Long, request: CrearPagoRequest): PagoDto {
        validar(request.monto > 0.0 && request.monto.isFinite()) { "El monto debe ser mayor a cero" }
        validar(request.monto <= MONTO_MAXIMO) { "El monto supera el máximo permitido" }
        validar(request.deudorId != request.acreedorId) { "El deudor y el acreedor deben ser distintos" }

        return consulta {
            val idsMiembros = miembrosOFallar(grupoId).map { it.id }.toSet()
            validar(request.deudorId in idsMiembros) { "deudorId no pertenece al grupo" }
            validar(request.acreedorId in idsMiembros) { "acreedorId no pertenece al grupo" }

            val pagoId = Pagos.insert {
                it[Pagos.grupoId] = grupoId
                it[deudorId] = request.deudorId
                it[acreedorId] = request.acreedorId
                it[monto] = request.monto.aCentavos().aBigDecimal()
                it[fecha] = request.fecha ?: System.currentTimeMillis()
            } get Pagos.id
            Pagos.selectAll().where { Pagos.id eq pagoId }.single().aPagoDto()
        }
    }

    suspend fun calcularSaldos(grupoId: Long): SaldosDto = consulta {
        val miembros = miembrosOFallar(grupoId)
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

    suspend fun calcularEstadisticas(grupoId: Long): EstadisticasDto = consulta {
        miembrosOFallar(grupoId)
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

    suspend fun obtenerActividad(grupoId: Long, limite: Int): List<ActividadDto> = consulta {
        val nombres = miembrosOFallar(grupoId).associate { it.id to it.nombre }
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

    private fun miembrosOFallar(grupoId: Long): List<MiembroDto> =
        grupoService.miembrosDe(grupoId) ?: throw RecursoNoEncontradoException("Grupo $grupoId no encontrado")

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
                participantes = participantesPorGasto[it[Gastos.id]].orEmpty()
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
