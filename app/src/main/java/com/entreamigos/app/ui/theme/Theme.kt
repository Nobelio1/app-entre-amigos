package com.entreamigos.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = VerdeInicio,
    onPrimary = SuperficieClara,
    secondary = MoradoAccion,
    onSecondary = SuperficieClara,
    background = FondoClaro,
    onBackground = TextoPrincipalClaro,
    surface = SuperficieClara,
    // Sin tinte tonal: el mockup usa tarjetas blancas puras (la profundidad la dan sombra y borde)
    surfaceTint = SuperficieClara,
    onSurface = TextoPrincipalClaro,
    onSurfaceVariant = TextoSecundarioClaro,
    outlineVariant = BordeTarjetaClaro,
    error = RojoSaldoNegativo
)

private val DarkColors = darkColorScheme(
    primary = VerdeMenta,
    onPrimary = VerdeInicioOscuro,
    secondary = MoradoGrupoClaro,
    onSecondary = SuperficieClara,
    background = FondoOscuro,
    surface = SuperficieOscura,
    onSurfaceVariant = TextoSecundarioOscuro,
    outlineVariant = BordeTarjetaOscuro,
    error = RojoSaldoNegativo
)

@Composable
fun EntreAmigosTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
