package com.entreamigos.backend.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BalanceCalculatorTest {

    private val ana = 1L
    private val luis = 2L
    private val carlos = 3L
    private val maria = 4L

    /** Registra un gasto dividido en partes iguales, como lo hace GastoService. */
    private fun gasto(pagadoPor: Long, soles: Double, vararg participantes: Long): Pair<GastoCalculo, List<ParticipacionCalculo>> {
        val centavos = soles.aCentavos()
        val reparto = BalanceCalculator.dividirEnPartesIguales(centavos, participantes.toList())
        return GastoCalculo(pagadoPor, centavos) to reparto.map { ParticipacionCalculo(it.key, it.value) }
    }

    private fun deudas(
        miembros: List<Long>,
        vararg gastos: Pair<GastoCalculo, List<ParticipacionCalculo>>,
        pagos: List<PagoCalculo> = emptyList()
    ): List<Transferencia> {
        val balances = BalanceCalculator.calcularBalancesNetos(
            miembros, gastos.map { it.first }, gastos.flatMap { it.second }, pagos
        )
        assertEquals(0L, balances.values.sum(), "La suma de balances debe ser exactamente cero")
        return BalanceCalculator.simplificarDeudas(balances)
    }

    @Test
    fun `deuda simple entre dos personas`() {
        val resultado = deudas(listOf(ana, luis), gasto(ana, 100.0, ana, luis))

        assertEquals(listOf(Transferencia(deudorId = luis, acreedorId = ana, montoCentavos = 50_00)), resultado)
    }

    @Test
    fun `varios deudores le pagan al unico acreedor`() {
        val resultado = deudas(listOf(ana, luis, carlos, maria), gasto(ana, 400.0, ana, luis, carlos, maria))

        assertEquals(3, resultado.size)
        assertTrue(resultado.all { it.acreedorId == ana && it.montoCentavos == 100_00L })
        assertEquals(setOf(luis, carlos, maria), resultado.map { it.deudorId }.toSet())
    }

    @Test
    fun `deudas circulares de igual monto se cancelan por completo`() {
        // Ana paga por Luis, Luis paga por Carlos, Carlos paga por Ana: A→B→C→A
        val resultado = deudas(
            listOf(ana, luis, carlos),
            gasto(ana, 30.0, luis),
            gasto(luis, 30.0, carlos),
            gasto(carlos, 30.0, ana)
        )

        assertTrue(resultado.isEmpty(), "No debería quedar ninguna deuda, pero hay $resultado")
    }

    @Test
    fun `deudas circulares de distinto monto se reducen a una sola transferencia`() {
        // Luis le debe 50 a Ana, Carlos le debe 30 a Luis y Ana le debe 20 a Carlos
        val resultado = deudas(
            listOf(ana, luis, carlos),
            gasto(ana, 50.0, luis),
            gasto(luis, 30.0, carlos),
            gasto(carlos, 20.0, ana)
        )

        // Netos: Ana +30, Luis -20, Carlos -10 → dos transferencias en vez de tres
        assertEquals(
            listOf(Transferencia(luis, ana, 20_00), Transferencia(carlos, ana, 10_00)),
            resultado
        )
    }

    @Test
    fun `montos con decimales se reparten sin perder ni inventar centimos`() {
        val reparto = BalanceCalculator.dividirEnPartesIguales(100.0.aCentavos(), listOf(ana, luis, carlos))

        assertEquals(mapOf(ana to 33_34L, luis to 33_33L, carlos to 33_33L), reparto)
        assertEquals(100_00L, reparto.values.sum())
    }

    @Test
    fun `montos que en Double acumulan error se calculan exactos`() {
        // 0.1 + 0.2 != 0.3 en punto flotante; en céntimos el resultado es exacto
        val resultado = deudas(
            listOf(ana, luis),
            gasto(ana, 0.10, luis),
            gasto(ana, 0.20, luis)
        )

        assertEquals(listOf(Transferencia(luis, ana, 30)), resultado)
    }

    @Test
    fun `reparto de 20 soles entre 3 no cobra un centimo de mas`() {
        val reparto = BalanceCalculator.dividirEnPartesIguales(20.0.aCentavos(), listOf(ana, luis, carlos))

        assertEquals(20_00L, reparto.values.sum())
        assertEquals(listOf(6_67L, 6_67L, 6_66L), reparto.values.toList())
    }

    @Test
    fun `un pago registrado reduce la deuda pendiente`() {
        val resultado = deudas(
            listOf(ana, luis, carlos),
            gasto(ana, 150.0, ana, luis, carlos),
            pagos = listOf(PagoCalculo(deudorId = luis, acreedorId = ana, montoCentavos = 50_00))
        )

        assertEquals(listOf(Transferencia(carlos, ana, 50_00)), resultado)
    }

    @Test
    fun `escenario del mockup con decimales produce saldos coherentes`() {
        val resultado = deudas(
            listOf(ana, luis, carlos, maria),
            gasto(ana, 150.0, ana, luis, carlos, maria),
            gasto(luis, 75.0, ana, luis, carlos, maria),
            gasto(carlos, 500.0, ana, luis, carlos, maria),
            gasto(maria, 33.33, ana, luis, carlos)
        )

        // Nunca más de N-1 transferencias y nadie se paga a sí mismo
        assertTrue(resultado.size <= 3)
        assertTrue(resultado.none { it.deudorId == it.acreedorId })
        assertTrue(resultado.all { it.montoCentavos > 0 })
        // Lo que reciben los acreedores cuadra con lo que entregan los deudores
        val totalTransferido = resultado.sumOf { it.montoCentavos }
        assertEquals(totalTransferido, resultado.groupBy { it.acreedorId }.values.sumOf { l -> l.sumOf { it.montoCentavos } })
    }

    @Test
    fun `conversion entre soles y centimos redondea a dos decimales`() {
        assertEquals(12_050L, 120.5.aCentavos())
        assertEquals(1L, 0.005.aCentavos())
        assertEquals(120.5, 12_050L.aSoles())
    }
}
