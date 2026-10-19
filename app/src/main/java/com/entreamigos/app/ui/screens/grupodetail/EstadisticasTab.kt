package com.entreamigos.app.ui.screens.grupodetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.entreamigos.app.ui.components.IconoCircular
import com.entreamigos.app.ui.components.estiloCategoria
import com.entreamigos.app.ui.components.nombreCategoria
import com.entreamigos.app.util.CurrencyFormatter
import com.entreamigos.app.viewmodel.GrupoDetailUiState

@Composable
fun EstadisticasTab(uiState: GrupoDetailUiState) {
    if (uiState.estadisticas.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Text(
                "Registra gastos para ver aquí tus estadísticas por categoría.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Text("Gasto por categoría", style = MaterialTheme.typography.titleMedium) }
        items(uiState.estadisticas) { estadistica ->
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconoCircular(estilo = estiloCategoria(estadistica.categoria), tamano = 28.dp)
                        Text(
                            text = nombreCategoria(estadistica.categoria),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    Text(CurrencyFormatter.formatear(estadistica.monto), style = MaterialTheme.typography.bodyMedium)
                }
                BarraDeProgreso(porcentaje = estadistica.porcentaje / 100f)
                Text(
                    text = "${"%.1f".format(estadistica.porcentaje)}% del total",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BarraDeProgreso(porcentaje: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(porcentaje.coerceIn(0f, 1f))
                .height(10.dp)
                .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(6.dp))
        )
    }
}
