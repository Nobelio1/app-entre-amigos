package com.entreamigos.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class BalanceCalculatorTest {

    private fun suma(reparto: List<Pair<Long, Double>>) = Math.round(reparto.sumOf { it.second } * 100)

    @Test
    fun `reparto exacto no agrega centimos`() {
        val reparto = BalanceCalculator.dividirEnPartesIguales(90.0, listOf(1L, 2L, 3L))
        assertEquals(listOf(30.0, 30.0, 30.0), reparto.map { it.second })
    }

    @Test
    fun `el primer participante absorbe el centimo sobrante`() {
        val reparto = BalanceCalculator.dividirEnPartesIguales(100.0, listOf(1L, 2L, 3L))
        assertEquals(listOf(1L to 33.34, 2L to 33.33, 3L to 33.33), reparto)
    }

    @Test
    fun `veinte soles entre tres suma exactamente veinte`() {
        val reparto = BalanceCalculator.dividirEnPartesIguales(20.0, listOf(1L, 2L, 3L))
        assertEquals(listOf(6.67, 6.67, 6.66), reparto.map { it.second })
        assertEquals(2000L, suma(reparto))
    }

    @Test
    fun `la suma coincide con el monto para muchos casos`() {
        for (centavos in 1..5000 step 7) {
            for (n in 1..7) {
                val monto = centavos / 100.0
                val reparto = BalanceCalculator.dividirEnPartesIguales(monto, (1L..n).toList())
                assertEquals("monto=$monto n=$n", centavos.toLong(), suma(reparto))
            }
        }
    }
}
