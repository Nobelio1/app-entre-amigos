package com.entreamigos.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.entreamigos.app.data.model.Deuda
import com.entreamigos.app.data.model.Gasto
import com.entreamigos.app.util.CurrencyFormatter
import com.entreamigos.app.util.DateFormatter

@Composable
fun GastoItem(gasto: Gasto, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .semantics {
                contentDescription = "Gasto ${gasto.descripcion}, ${CurrencyFormatter.formatear(gasto.monto)}, " +
                    "pagado por ${gasto.pagadoPor.nombre}"
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconoCircular(estilo = estiloCategoria(gasto.categoria))
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(text = gasto.descripcion, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "Pagado por ${gasto.pagadoPor.nombre} · ${DateFormatter.formatear(gasto.fecha)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Dividido entre ${gasto.participantes.size} persona${if (gasto.participantes.size == 1) "" else "s"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(text = CurrencyFormatter.formatear(gasto.monto), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun DeudaItem(
    deuda: Deuda,
    esDeudaHaciaElUsuario: Boolean,
    onLiquidar: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val otroMiembro = if (esDeudaHaciaElUsuario) deuda.deudor else deuda.acreedor
    val etiqueta = if (esDeudaHaciaElUsuario) "Te debe" else "Le debes"

    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarMiembro(miembro = otroMiembro)
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(text = otroMiembro.nombre, style = MaterialTheme.typography.bodyLarge)
            Text(text = etiqueta, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            text = CurrencyFormatter.formatear(deuda.monto),
            style = MaterialTheme.typography.titleMedium,
            color = colorDeBalance(if (esDeudaHaciaElUsuario) deuda.monto else -deuda.monto)
        )
    }
    if (onLiquidar != null) {
        HorizontalDivider()
    }
}
