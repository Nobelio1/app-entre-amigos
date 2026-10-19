package com.entreamigos.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.entreamigos.app.ui.theme.VerdeAccion

enum class BottomDestino(
    val etiqueta: String,
    val icono: ImageVector,
    val iconoSeleccionado: ImageVector,
    val ruta: String
) {
    INICIO("Inicio", Icons.Outlined.Home, Icons.Filled.Home, Screen.Home.route),
    GRUPOS("Grupos", Icons.Outlined.Groups, Icons.Filled.Groups, "grupos"),
    CREAR("Crear", Icons.Filled.Add, Icons.Filled.Add, Screen.CrearGrupo.route),
    GASTOS("Gastos", Icons.AutoMirrored.Outlined.ReceiptLong, Icons.AutoMirrored.Filled.ReceiptLong, "gastos"),
    PERFIL("Perfil", Icons.Outlined.Person, Icons.Filled.Person, Screen.Perfil.route)
}

/**
 * Barra inferior del mockup: fondo blanco con esquinas superiores redondeadas, sin la
 * "píldora" de selección de Material 3 (el destino activo solo cambia a ícono relleno y
 * color verde) y la acción "Crear" como botón circular verde en el centro, sin etiqueta.
 */
@Composable
fun EntreAmigosBottomBar(rutaActual: String?, onNavegar: (BottomDestino) -> Unit) {
    val colorActivo = MaterialTheme.colorScheme.primary
    val colores = NavigationBarItemDefaults.colors(
        selectedIconColor = colorActivo,
        selectedTextColor = colorActivo,
        indicatorColor = Color.Transparent,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
    NavigationBar(
        modifier = Modifier.clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        BottomDestino.entries.forEach { destino ->
            if (destino == BottomDestino.CREAR) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    FloatingActionButton(
                        onClick = { onNavegar(destino) },
                        shape = CircleShape,
                        containerColor = VerdeAccion,
                        contentColor = Color.White,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(destino.icono, contentDescription = "Crear grupo", modifier = Modifier.size(28.dp))
                    }
                }
            } else {
                val seleccionado = rutaActual == destino.ruta
                NavigationBarItem(
                    selected = seleccionado,
                    onClick = { onNavegar(destino) },
                    icon = {
                        Icon(
                            if (seleccionado) destino.iconoSeleccionado else destino.icono,
                            contentDescription = null
                        )
                    },
                    label = { Text(destino.etiqueta, style = MaterialTheme.typography.labelSmall) },
                    colors = colores
                )
            }
        }
    }
}
