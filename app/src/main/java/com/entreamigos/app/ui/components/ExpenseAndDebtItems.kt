package com.entreamigos.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.entreamigos.app.data.model.ActividadItem
import com.entreamigos.app.data.model.Deuda
import com.entreamigos.app.data.model.Gasto
import com.entreamigos.app.data.model.Miembro
import com.entreamigos.app.util.CurrencyFormatter
import com.entreamigos.app.util.DateFormatter

/** "Pagado por Ana" / "Pagado por ti" (el mockup usa "tú", pero tras preposición va "ti"). */
fun textoPagadoPor(miembro: Miembro): String =
    if (miembro.esUsuarioActual) "Pagado por ti" else "Pagado por ${miembro.nombre}"

fun textoDivididoEntre(personas: Int): String =
    "Dividido entre $personas persona" + if (personas == 1) "" else "s"

/**
 * Fila de gasto común a Home y a la actividad del grupo: ícono de categoría, título,
 * líneas secundarias, y monto + fecha a la derecha.
 */
@Composable
private fun FilaGasto(
    gasto: Gasto,
    lineasSecundarias: List<String>,
    colorMonto: Color,
    modifier: Modifier = Modifier
) {
    val monto = CurrencyFormatter.formatear(gasto.monto)
    val fecha = DateFormatter.formatear(gasto.fecha)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .clearAndSetSemantics {
                contentDescription = "Gasto ${gasto.descripcion}, $monto, ${lineasSecundarias.joinToString()}, $fecha"
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconoCircular(estilo = estiloCategoria(gasto.categoria), tamano = 40.dp)
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
            Text(
                text = gasto.descripcion,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            lineasSecundarias.forEach {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(text = monto, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = colorMonto)
            Text(text = fecha, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Gasto dentro de un grupo (pestañas Resumen y Gastos). En el mockup, el monto va en morado. */
@Composable
fun GastoItem(gasto: Gasto, modifier: Modifier = Modifier, colorMonto: Color = MaterialTheme.colorScheme.secondary) {
    FilaGasto(
        gasto = gasto,
        lineasSecundarias = listOf(textoPagadoPor(gasto.pagadoPor), textoDivididoEntre(gasto.participantes.size)),
        colorMonto = colorMonto,
        modifier = modifier
    )
}

/** Gasto en "Gastos recientes" de Home: muestra a qué grupo pertenece. Monto en verde. */
@Composable
fun GastoRecienteItem(item: ActividadItem, modifier: Modifier = Modifier) {
    FilaGasto(
        gasto = item.gasto,
        lineasSecundarias = listOf(item.nombreGrupo, textoPagadoPor(item.gasto.pagadoPor)),
        colorMonto = colorDeBalance(1.0),
        modifier = modifier
    )
}

/** Fila de "¿Quién debe a quién?": avatar, nombre, relación con el usuario, monto y chevron. */
@Composable
fun DeudaItem(
    deuda: Deuda,
    esDeudaHaciaElUsuario: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val otroMiembro = if (esDeudaHaciaElUsuario) deuda.deudor else deuda.acreedor
    val etiqueta = if (esDeudaHaciaElUsuario) "Te debe" else "Tú debes"
    val monto = CurrencyFormatter.formatear(deuda.monto)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .clearAndSetSemantics { contentDescription = "${otroMiembro.nombre}, $etiqueta, $monto" },
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarMiembro(miembro = otroMiembro)
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(text = otroMiembro.nombre, style = MaterialTheme.typography.titleSmall)
            Text(text = etiqueta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            text = monto,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = colorDeBalance(if (esDeudaHaciaElUsuario) deuda.monto else -deuda.monto)
        )
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}
