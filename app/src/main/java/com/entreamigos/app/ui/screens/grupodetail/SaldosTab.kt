package com.entreamigos.app.ui.screens.grupodetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.entreamigos.app.data.model.Deuda
import com.entreamigos.app.ui.components.AvatarMiembro
import com.entreamigos.app.ui.components.colorDeBalance
import com.entreamigos.app.ui.theme.VerdeSaldoPositivo
import com.entreamigos.app.util.CurrencyFormatter
import com.entreamigos.app.viewmodel.GrupoDetailUiState

@Composable
fun SaldosTab(uiState: GrupoDetailUiState, onLiquidar: (Deuda) -> Unit) {
    var deudaSeleccionada by remember { mutableStateOf<Deuda?>(null) }

    if (uiState.deudas.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.TaskAlt,
                contentDescription = null,
                tint = VerdeSaldoPositivo,
                modifier = Modifier.size(56.dp)
            )
            Text(
                "¡Todos los saldos están en cero!",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 12.dp)
            )
            Text(
                "Nadie le debe nada a nadie en este grupo.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                Text("Deudas pendientes en el grupo", style = MaterialTheme.typography.titleMedium)
            }
            items(uiState.deudas) { deuda ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    AvatarMiembro(miembro = deuda.deudor)
                    Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                        Text("${deuda.deudor.nombre} → ${deuda.acreedor.nombre}")
                        Text(
                            CurrencyFormatter.formatear(deuda.monto),
                            color = colorDeBalance(-deuda.monto),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    TextButton(onClick = { deudaSeleccionada = deuda }) {
                        Text("Marcar pagada")
                    }
                }
            }
            item {
                Button(
                    onClick = { uiState.deudas.firstOrNull()?.let { deudaSeleccionada = it } },
                    enabled = uiState.deudas.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Liquidar deudas")
                }
            }
        }
    }

    val deuda = deudaSeleccionada
    if (deuda != null) {
        AlertDialog(
            onDismissRequest = { deudaSeleccionada = null },
            title = { Text("Confirmar pago") },
            text = {
                Text(
                    "¿Confirmas que ${deuda.deudor.nombre} le pagó " +
                        "${CurrencyFormatter.formatear(deuda.monto)} a ${deuda.acreedor.nombre}?"
                )
            },
            confirmButton = {
                Button(onClick = {
                    onLiquidar(deuda)
                    deudaSeleccionada = null
                }) { Text("Sí, marcar como pagada") }
            },
            dismissButton = {
                OutlinedButton(onClick = { deudaSeleccionada = null }) { Text("Cancelar") }
            }
        )
    }
}
