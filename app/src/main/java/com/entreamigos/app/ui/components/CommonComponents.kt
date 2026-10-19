package com.entreamigos.app.ui.components

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.entreamigos.app.data.model.Miembro
import com.entreamigos.app.ui.theme.RojoSaldoNegativo
import com.entreamigos.app.ui.theme.VerdeSaldoPositivo

@Composable
fun AvatarInicial(nombre: String, colorHex: String, modifier: Modifier = Modifier, tamano: androidx.compose.ui.unit.Dp = 40.dp) {
    val color = runCatching { Color(android.graphics.Color.parseColor(colorHex)) }
        .getOrDefault(MaterialTheme.colorScheme.secondary)
    Box(
        modifier = modifier
            .size(tamano)
            .background(color, CircleShape)
            .semantics { contentDescription = "Avatar de $nombre" },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = nombre.trim().firstOrNull()?.uppercase() ?: "?",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = (tamano.value * 0.42f).sp)
        )
    }
}

@Composable
fun AvatarMiembro(miembro: Miembro, modifier: Modifier = Modifier) {
    AvatarInicial(nombre = miembro.nombre, colorHex = miembro.colorAvatarHex, modifier = modifier)
}

/** Verde/rojo de saldos; en modo oscuro se aclaran para mantener contraste AA. */
@Composable
fun colorDeBalance(monto: Double): Color {
    val oscuro = isSystemInDarkTheme()
    return when {
        monto > 0.005 -> if (oscuro) Color(0xFF66BB6A) else VerdeSaldoPositivo
        monto < -0.005 -> if (oscuro) Color(0xFFEF5350) else RojoSaldoNegativo
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

/** Forma y borde comunes de las tarjetas del mockup (blancas, esquinas de 14dp, borde sutil). */
val FormaTarjeta = RoundedCornerShape(14.dp)

/**
 * Tarjeta estándar de la app. El mockup usa una sombra muy suave más un borde de 1dp;
 * con solo elevación, en pantallas de bajo contraste la tarjeta se pierde en el fondo.
 */
@Composable
fun TarjetaEntreAmigos(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contenido: @Composable ColumnScope.() -> Unit
) {
    val colores = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    val elevacion = CardDefaults.cardElevation(defaultElevation = 1.dp)
    val borde = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, shape = FormaTarjeta, colors = colores, elevation = elevacion, border = borde, content = contenido)
    } else {
        Card(modifier = modifier, shape = FormaTarjeta, colors = colores, elevation = elevacion, border = borde, content = contenido)
    }
}

/** Título de sección con acción opcional a la derecha ("Ver todos", "Crear grupo"). */
@Composable
fun EncabezadoSeccion(
    titulo: String,
    modifier: Modifier = Modifier,
    accion: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = 40.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f).semantics { heading() }
        )
        accion?.invoke()
    }
}

/** Acción de texto compacta para [EncabezadoSeccion]. */
@Composable
fun AccionSeccion(texto: String, color: Color, onClick: () -> Unit, icono: ImageVector? = null) {
    TextButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 8.dp)) {
        if (icono != null) {
            Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(18.dp).padding(end = 2.dp))
        }
        Text(texto, color = color, style = MaterialTheme.typography.labelMedium)
    }
}

/**
 * Pide íconos claros u oscuros en la barra de estado mientras la pantalla está visible
 * y restaura el valor anterior al salir. Necesario con edge-to-edge: Home y Detalle
 * pintan su header (oscuro) detrás de la barra de estado; el resto de pantallas no.
 */
@Composable
fun IconosBarraDeEstado(claros: Boolean) {
    val vista = LocalView.current
    if (vista.isInEditMode) return
    DisposableEffect(claros) {
        val ventana = (vista.context as? Activity)?.window ?: return@DisposableEffect onDispose { }
        val controlador = WindowCompat.getInsetsController(ventana, vista)
        val anterior = controlador.isAppearanceLightStatusBars
        controlador.isAppearanceLightStatusBars = !claros
        onDispose { controlador.isAppearanceLightStatusBars = anterior }
    }
}
