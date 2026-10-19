package com.entreamigos.backend.domain

import java.math.BigDecimal
import java.math.RoundingMode

/** Gasto reducido a lo que importa para calcular saldos. Montos en céntimos. */
data class GastoCalculo(val pagadoPorId: Long, val montoCentavos: Long)

/** Parte de un gasto asignada a un miembro. Monto en céntimos. */
data class ParticipacionCalculo(val miembroId: Long, val montoCentavos: Long)

/** Pago de liquidación ya registrado (el deudor le entregó dinero al acreedor). */
data class PagoCalculo(val deudorId: Long, val acreedorId: Long, val montoCentavos: Long)

/** Transferencia sugerida para dejar el grupo en cero. */
data class Transferencia(val deudorId: Long, val acreedorId: Long, val montoCentavos: Long)

/**
 * Port al servidor de `util/BalanceCalculator.kt` de la app Android.
 *
 * El algoritmo es el mismo (balance neto por miembro + emparejamiento greedy del
 * mayor deudor con el mayor acreedor), pero aquí se trabaja en **céntimos enteros**
 * en lugar de `Double`: así la suma de balances es exactamente cero y no hace falta
 * un EPSILON para descartar residuos de punto flotante (p. ej. 0.1 + 0.2).
 */
object BalanceCalculator {

    /**
     * Balance neto de cada miembro: positivo = le deben, negativo = debe.
     * Un pago suma al deudor (ya cubrió parte de lo que debía) y resta al acreedor.
     */
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

    /**
     * Reduce los balances a la menor cantidad práctica de transferencias.
     *
     * Greedy: en cada paso el mayor deudor le paga al mayor acreedor el mínimo entre
     * ambos saldos, y el que queda en cero sale de la lista. Produce como máximo
     * N-1 transferencias y resuelve automáticamente las deudas circulares
     * (A→B→C→A se cancelan en el balance neto antes de emparejar).
     * Los empates se rompen por id para que la respuesta sea determinista.
     */
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

    /**
     * Divide un monto en partes iguales sin perder ni inventar céntimos: los primeros
     * `resto` participantes (en el orden recibido) absorben un céntimo extra.
     * Ej.: 100.00 entre 3 → 33.34, 33.33, 33.33.
     */
    fun dividirEnPartesIguales(montoCentavos: Long, participantesIds: List<Long>): Map<Long, Long> {
        require(participantesIds.isNotEmpty()) { "Se necesita al menos un participante" }
        val base = montoCentavos / participantesIds.size
        val resto = montoCentavos % participantesIds.size
        return participantesIds.withIndex().associate { (indice, id) ->
            id to base + if (indice < resto) 1 else 0
        }
    }
}

/** Conversión segura entre la representación de la API (soles en decimal) y céntimos. */
fun Double.aCentavos(): Long = BigDecimal.valueOf(this).setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact()

fun BigDecimal.aCentavos(): Long = setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact()

fun Long.aSoles(): Double = BigDecimal.valueOf(this, 2).toDouble()

fun Long.aBigDecimal(): BigDecimal = BigDecimal.valueOf(this, 2)
