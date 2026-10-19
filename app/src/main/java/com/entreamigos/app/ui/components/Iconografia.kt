package com.entreamigos.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.LocalPizza
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.entreamigos.app.data.local.entity.CategoriaGasto
import com.entreamigos.app.data.local.entity.GrupoIcono

/*
 * Única fuente de iconografía de la app: todo ícono de grupo o categoría pasa por
 * aquí. La app no usa emojis porque su dibujo depende de la fuente del dispositivo;
 * los ImageVector de material-icons-extended se ven idénticos en todos lados.
 */

/** Ícono y color de acento de un grupo. */
data class EstiloIcono(val icono: ImageVector, val color: Color, val etiqueta: String)

fun estiloGrupo(icono: GrupoIcono): EstiloIcono = when (icono) {
    GrupoIcono.VIAJE -> EstiloIcono(Icons.Filled.BeachAccess, Color(0xFF1E88E5), "Viaje")
    GrupoIcono.CASA -> EstiloIcono(Icons.Filled.Home, Color(0xFF43A047), "Casa compartida")
    GrupoIcono.SALIDAS -> EstiloIcono(Icons.Filled.LocalBar, Color(0xFFF59E0B), "Salidas")
    GrupoIcono.ESTUDIOS -> EstiloIcono(Icons.Filled.School, Color(0xFF5E35B1), "Estudios")
    GrupoIcono.AUTO -> EstiloIcono(Icons.Filled.DirectionsCar, Color(0xFF00838F), "Auto")
    GrupoIcono.FIESTA -> EstiloIcono(Icons.Filled.Celebration, Color(0xFFD81B60), "Fiesta")
    GrupoIcono.COMIDA -> EstiloIcono(Icons.Filled.LocalPizza, Color(0xFFE53935), "Comida")
    GrupoIcono.DEPORTE -> EstiloIcono(Icons.Filled.SportsSoccer, Color(0xFF558B2F), "Deporte")
    GrupoIcono.GENERAL -> EstiloIcono(Icons.Filled.Groups, Color(0xFF546E7A), "General")
}

/** Colores de categoría tomados del mockup (círculos de "Gastos recientes"). */
fun estiloCategoria(categoria: CategoriaGasto): EstiloIcono = when (categoria) {
    CategoriaGasto.COMIDA -> EstiloIcono(Icons.Filled.Restaurant, Color(0xFFEF7D2D), "Comida")
    CategoriaGasto.TRANSPORTE -> EstiloIcono(Icons.Filled.DirectionsCar, Color(0xFF43A047), "Transporte")
    CategoriaGasto.HOSPEDAJE -> EstiloIcono(Icons.Filled.Hotel, Color(0xFF3F7BE0), "Hospedaje")
    CategoriaGasto.ENTRETENIMIENTO -> EstiloIcono(Icons.Filled.Celebration, Color(0xFF8E5BD9), "Entretenimiento")
    CategoriaGasto.SERVICIOS -> EstiloIcono(Icons.Filled.Bolt, Color(0xFFF2A900), "Servicios")
    CategoriaGasto.OTROS -> EstiloIcono(Icons.AutoMirrored.Filled.ReceiptLong, Color(0xFF607D8B), "Otros")
}

fun iconoParaCategoria(categoria: CategoriaGasto): ImageVector = estiloCategoria(categoria).icono

fun nombreCategoria(categoria: CategoriaGasto): String = estiloCategoria(categoria).etiqueta

/**
 * Ícono dentro de un círculo. Dos variantes del mockup:
 * - `relleno = true`: círculo sólido con ícono blanco (categorías de gasto).
 * - `relleno = false`: círculo pastel (color al 15 %) con ícono de color (grupos).
 *   Usar alfa en vez de un pastel fijo hace que también funcione en modo oscuro.
 */
@Composable
fun IconoCircular(
    estilo: EstiloIcono,
    modifier: Modifier = Modifier,
    tamano: Dp = 40.dp,
    relleno: Boolean = true
) {
    Box(
        modifier = modifier
            .size(tamano)
            .background(if (relleno) estilo.color else estilo.color.copy(alpha = 0.15f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = estilo.icono,
            contentDescription = estilo.etiqueta,
            tint = if (relleno) Color.White else estilo.color,
            modifier = Modifier.size(tamano * 0.55f)
        )
    }
}
