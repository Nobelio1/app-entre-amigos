package com.entreamigos.app.util

import com.entreamigos.app.data.local.entity.GastoEntity
import com.entreamigos.app.data.local.entity.GastoParticipanteEntity
import com.entreamigos.app.data.local.entity.MiembroEntity
import com.entreamigos.app.data.local.entity.PagoEntity
import com.entreamigos.app.data.model.Deuda
import com.entreamigos.app.data.model.Miembro
import kotlin.math.abs
import kotlin.math.min

object BalanceCalculator {

    private const val EPSILON = 0.01

    fun calcularBalancesNetos(
        miembros: List<MiembroEntity>,
        gastos: List<GastoEntity>,
        participaciones: List<GastoParticipanteEntity>,
        pagos: List<PagoEntity>
    ): Map<Long, Double> {
        val neto = miembros.associate { it.id to 0.0 }.toMutableMap()

        gastos.forEach { gasto ->
            neto[gasto.pagadoPorId] = (neto[gasto.pagadoPorId] ?: 0.0) + gasto.monto
        }
        participaciones.forEach { participacion ->
            neto[participacion.miembroId] = (neto[participacion.miembroId] ?: 0.0) - participacion.montoAsignado
        }
        pagos.forEach { pago ->
            neto[pago.deudorId] = (neto[pago.deudorId] ?: 0.0) + pago.monto
            neto[pago.acreedorId] = (neto[pago.acreedorId] ?: 0.0) - pago.monto
        }
        return neto
    }

    fun simplificarDeudas(
        miembros: List<MiembroEntity>,
        balancesNetos: Map<Long, Double>
    ): List<Deuda> {
        val miembroPorId = miembros.associateBy { it.id }

        data class Saldo(val miembroId: Long, var monto: Double)

        val deudores = balancesNetos.filter { it.value < -EPSILON }
            .map { Saldo(it.key, -it.value) }
            .sortedByDescending { it.monto }
            .toMutableList()
        val acreedores = balancesNetos.filter { it.value > EPSILON }
            .map { Saldo(it.key, it.value) }
            .sortedByDescending { it.monto }
            .toMutableList()

        val resultado = mutableListOf<Deuda>()
        var i = 0
        var j = 0
        while (i < deudores.size && j < acreedores.size) {
            val deudor = deudores[i]
            val acreedor = acreedores[j]
            val monto = min(deudor.monto, acreedor.monto)

            if (monto > EPSILON) {
                val miembroDeudor = miembroPorId[deudor.miembroId]
                val miembroAcreedor = miembroPorId[acreedor.miembroId]
                if (miembroDeudor != null && miembroAcreedor != null) {
                    resultado += Deuda(
                        deudor = miembroDeudor.toModel(),
                        acreedor = miembroAcreedor.toModel(),
                        monto = redondear(monto)
                    )
                }
            }

            deudor.monto -= monto
            acreedor.monto -= monto
            if (abs(deudor.monto) < EPSILON) i++
            if (abs(acreedor.monto) < EPSILON) j++
        }
        return resultado
    }

    /**
     * Divide [monto] en partes iguales trabajando en céntimos enteros: los primeros
     * participantes (en el orden recibido) absorben el céntimo sobrante, de modo que
     * la suma de las partes siempre es exactamente el monto (100 / 3 → 33.34, 33.33, 33.33).
     *
     * Es la misma regla que aplica el backend (`backend/.../domain/BalanceCalculator.kt`),
     * así el reparto calculado offline coincide con el que devuelve el servidor.
     */
    fun dividirEnPartesIguales(monto: Double, participantesIds: List<Long>): List<Pair<Long, Double>> {
        require(participantesIds.isNotEmpty()) { "Se necesita al menos un participante" }
        val totalCentavos = Math.round(monto * 100)
        val base = totalCentavos / participantesIds.size
        val resto = totalCentavos % participantesIds.size
        return participantesIds.mapIndexed { indice, id ->
            val centavos = base + if (indice < resto) 1 else 0
            id to centavos / 100.0
        }
    }

    fun redondear(valor: Double): Double = Math.round(valor * 100.0) / 100.0

    private fun MiembroEntity.toModel() = Miembro(
        id = id,
        nombre = nombre,
        esUsuarioActual = esUsuarioActual,
        colorAvatarHex = colorAvatarHex
    )
}
