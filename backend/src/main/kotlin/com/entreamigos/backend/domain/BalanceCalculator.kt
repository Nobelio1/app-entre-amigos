package com.entreamigos.backend.domain

import java.math.BigDecimal
import java.math.RoundingMode

data class GastoCalculo(val pagadoPorId: Long, val montoCentavos: Long)

data class ParticipacionCalculo(val miembroId: Long, val montoCentavos: Long)

data class PagoCalculo(val deudorId: Long, val acreedorId: Long, val montoCentavos: Long)

data class Transferencia(val deudorId: Long, val acreedorId: Long, val montoCentavos: Long)

object BalanceCalculator {

    fun calcularBalancesNetos(
        miembroIds: Collection<Long>,
        gastos: List<GastoCalculo>,
        participaciones: List<ParticipacionCalculo>,
        pagos: List<PagoCalculo>
    ): Map<Long, Long> {
        val neto = miembroIds.associateWith { 0L }.toMutableMap()
        gastos.forEach { neto.merge(it.pagadoPorId, it.montoCentavos, Long::plus) }
        participaciones.forEach { neto.merge(it.miembroId, -it.montoCentavos, Long::plus) }
        pagos.forEach {
            neto.merge(it.deudorId, it.montoCentavos, Long::plus)
            neto.merge(it.acreedorId, -it.montoCentavos, Long::plus)
        }
        return neto
    }

    fun simplificarDeudas(balancesNetos: Map<Long, Long>): List<Transferencia> {
        class Saldo(val miembroId: Long, var centavos: Long)

        val orden = compareByDescending<Saldo> { it.centavos }.thenBy { it.miembroId }
        val deudores = balancesNetos.filterValues { it < 0 }
            .map { Saldo(it.key, -it.value) }
            .sortedWith(orden)
        val acreedores = balancesNetos.filterValues { it > 0 }
            .map { Saldo(it.key, it.value) }
            .sortedWith(orden)

        val resultado = mutableListOf<Transferencia>()
        var i = 0
        var j = 0
        while (i < deudores.size && j < acreedores.size) {
            val deudor = deudores[i]
            val acreedor = acreedores[j]
            val monto = minOf(deudor.centavos, acreedor.centavos)
            if (monto > 0) resultado += Transferencia(deudor.miembroId, acreedor.miembroId, monto)
            deudor.centavos -= monto
            acreedor.centavos -= monto
            if (deudor.centavos == 0L) i++
            if (acreedor.centavos == 0L) j++
        }
        return resultado
    }

    fun dividirEnPartesIguales(montoCentavos: Long, participantesIds: List<Long>): Map<Long, Long> {
        require(participantesIds.isNotEmpty()) { "Se necesita al menos un participante" }
        val base = montoCentavos / participantesIds.size
        val resto = montoCentavos % participantesIds.size
        return participantesIds.withIndex().associate { (indice, id) ->
            id to base + if (indice < resto) 1 else 0
        }
    }
}

fun Double.aCentavos(): Long = BigDecimal.valueOf(this).setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact()

fun BigDecimal.aCentavos(): Long = setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact()

fun Long.aSoles(): Double = BigDecimal.valueOf(this, 2).toDouble()

fun Long.aBigDecimal(): BigDecimal = BigDecimal.valueOf(this, 2)
