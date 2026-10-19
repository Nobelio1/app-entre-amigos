package com.entreamigos.app.ui.screens.grupodetail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.entreamigos.app.ui.components.GastoItem
import com.entreamigos.app.ui.components.TarjetaEntreAmigos
import com.entreamigos.app.viewmodel.GrupoDetailUiState

@Composable
fun GastosTab(uiState: GrupoDetailUiState, onRegistrarGastoClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 88.dp), modifier = Modifier.fillMaxSize()) {
            item {
                TarjetaEntreAmigos(modifier = Modifier.fillMaxWidth()) {
                    if (uiState.gastos.isEmpty()) {
                        Text(
                            text = "Aún no hay gastos registrados en este grupo.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    uiState.gastos.forEachIndexed { indice, gasto ->
                        if (indice > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        GastoItem(gasto = gasto)
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onRegistrarGastoClick,
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = Color.White,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Registrar nuevo gasto")
        }
    }
}
