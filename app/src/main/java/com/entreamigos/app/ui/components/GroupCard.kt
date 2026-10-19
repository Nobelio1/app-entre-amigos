package com.entreamigos.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.entreamigos.app.data.model.ResumenGrupo
import com.entreamigos.app.util.CurrencyFormatter

@Composable
fun GroupCard(resumen: ResumenGrupo, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val textoBalance = when {
        resumen.balanceUsuarioActual > 0.005 ->
            "Tu balance: ${CurrencyFormatter.formatear(resumen.balanceUsuarioActual)} te deben"
        resumen.balanceUsuarioActual < -0.005 ->
            "Tu balance: ${CurrencyFormatter.formatear(-resumen.balanceUsuarioActual)} debes"
        else -> "Estás al día"
    }

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Grupo ${resumen.nombre}, $textoBalance" },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconoCircular(estilo = estiloGrupo(resumen.icono), tamano = 48.dp, relleno = false)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(text = resumen.nombre, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${resumen.numeroMiembros} miembros",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = textoBalance,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorDeBalance(resumen.balanceUsuarioActual)
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
