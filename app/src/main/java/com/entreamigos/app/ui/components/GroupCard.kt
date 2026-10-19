package com.entreamigos.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.entreamigos.app.data.model.ResumenGrupo
import com.entreamigos.app.util.CurrencyFormatter

@Composable
fun GroupCard(resumen: ResumenGrupo, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val balance = resumen.balanceUsuarioActual
    val (montoTexto, sufijo) = when {
        balance > 0.005 -> CurrencyFormatter.formatear(balance) to "te deben"
        balance < -0.005 -> CurrencyFormatter.formatear(-balance) to "debes"
        else -> null to "estás al día"
    }
    val colorBalance = colorDeBalance(balance)
    val descripcion = "Grupo ${resumen.nombre}, ${resumen.numeroMiembros} miembros, " +
        if (montoTexto != null) "tu balance: $montoTexto $sufijo" else sufijo

    TarjetaEntreAmigos(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = descripcion }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconoCircular(estilo = estiloGrupo(resumen.icono), tamano = 56.dp, relleno = false)
            Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                Text(text = resumen.nombre, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                Text(
                    text = "${resumen.numeroMiembros} miembros",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) { append("Tu balance: ") }
                        withStyle(SpanStyle(color = colorBalance, fontWeight = FontWeight.SemiBold)) {
                            if (montoTexto != null) append("$montoTexto  ")
                            append(sufijo)
                        }
                    },
                    style = MaterialTheme.typography.bodySmall
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
