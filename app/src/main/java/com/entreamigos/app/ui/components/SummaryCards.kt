package com.entreamigos.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.entreamigos.app.data.model.ResumenGeneral
import com.entreamigos.app.ui.theme.AzulInformativo
import com.entreamigos.app.ui.theme.RojoSaldoNegativo
import com.entreamigos.app.ui.theme.VerdeAccion
import com.entreamigos.app.util.CurrencyFormatter

@Composable
fun ResumenGeneralRow(resumen: ResumenGeneral, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MiniResumenCard("Total gastado", resumen.totalGastado, Icons.Filled.NorthEast, VerdeAccion, Modifier.weight(1f))
        MiniResumenCard("Te deben", resumen.totalTeDeben, Icons.Filled.CurrencyExchange, AzulInformativo, Modifier.weight(1f))
        MiniResumenCard("Debes", resumen.totalDebes, Icons.Filled.SwapHoriz, RojoSaldoNegativo, Modifier.weight(1f))
    }
}

@Composable
private fun MiniResumenCard(titulo: String, monto: Double, icono: ImageVector, color: Color, modifier: Modifier = Modifier) {
    val valor = CurrencyFormatter.formatear(monto)
    TarjetaEntreAmigos(modifier = modifier.clearAndSetSemantics { contentDescription = "$titulo: $valor" }) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconoCircular(estilo = EstiloIcono(icono, color, titulo), tamano = 36.dp)
            Spacer(Modifier.height(10.dp))
            Text(text = valor, style = MaterialTheme.typography.titleSmall, color = color, maxLines = 1, textAlign = TextAlign.Center)
            Text(
                text = titulo,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
