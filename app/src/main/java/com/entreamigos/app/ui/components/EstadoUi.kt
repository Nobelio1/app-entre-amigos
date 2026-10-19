package com.entreamigos.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.entreamigos.app.data.model.EstadoConexion

@Composable
fun BannerConexion(estado: EstadoConexion, onReintentar: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        AnimatedVisibility(visible = estado.sincronizando) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        when {
            estado.sinConexion -> FranjaAviso(
                icono = { Icon(Icons.Filled.CloudOff, contentDescription = null, modifier = Modifier.size(18.dp)) },
                texto = buildString {
                    append("Modo offline")
                    if (estado.cambiosPendientes > 0) {
                        append(" · ${estado.cambiosPendientes} cambio")
                        if (estado.cambiosPendientes != 1) append("s")
                        append(" por sincronizar")
                    }
                },
                fondo = MaterialTheme.colorScheme.surfaceVariant,
                contenido = MaterialTheme.colorScheme.onSurfaceVariant,
                accionHabilitada = !estado.sincronizando,
                onReintentar = onReintentar
            )
            estado.mensajeError != null -> FranjaAviso(
                icono = { Icon(Icons.Filled.ErrorOutline, contentDescription = null, modifier = Modifier.size(18.dp)) },
                texto = estado.mensajeError!!,
                fondo = MaterialTheme.colorScheme.errorContainer,
                contenido = MaterialTheme.colorScheme.onErrorContainer,
                accionHabilitada = !estado.sincronizando,
                onReintentar = onReintentar
            )
        }
    }
}

@Composable
private fun FranjaAviso(
    icono: @Composable () -> Unit,
    texto: String,
    fondo: Color,
    contenido: Color,
    accionHabilitada: Boolean,
    onReintentar: () -> Unit
) {
    Surface(color = fondo, contentColor = contenido, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(start = 16.dp, end = 4.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            verticalAlignment = Alignment.CenterVertically
        ) {
            icono()
            Text(
                text = texto,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
            )
            TextButton(onClick = onReintentar, enabled = accionHabilitada) {
                Text("Reintentar", color = contenido)
            }
        }
    }
}

@Composable
fun PantallaCargando(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun PantallaError(mensaje: String, onReintentar: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp)
        )
        Text(mensaje, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onReintentar) { Text("Reintentar") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContenedorRefrescable(
    refrescando: Boolean,
    onRefrescar: () -> Unit,
    modifier: Modifier = Modifier,
    contenido: @Composable BoxScope.() -> Unit
) {
    val estado = rememberPullToRefreshState()
    if (estado.isRefreshing) {
        LaunchedEffect(Unit) { onRefrescar() }
    }
    LaunchedEffect(refrescando) {
        if (refrescando) estado.startRefresh() else estado.endRefresh()
    }
    Box(modifier = modifier.clipToBounds().nestedScroll(estado.nestedScrollConnection)) {
        contenido()
        PullToRefreshContainer(state = estado, modifier = Modifier.align(Alignment.TopCenter))
    }
}
