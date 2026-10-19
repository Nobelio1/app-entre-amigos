package com.entreamigos.app.ui.screens.gastos

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.entreamigos.app.data.repository.GastoRepository
import com.entreamigos.app.ui.components.GastoItem
import com.entreamigos.app.ui.components.estiloGrupo
import com.entreamigos.app.viewmodel.GastosGlobalViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GastosGlobalScreen(gastoRepository: GastoRepository) {
    val viewModel: GastosGlobalViewModel = viewModel(factory = GastosGlobalViewModel.Factory(gastoRepository))
    val actividad by viewModel.actividad.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Todos tus gastos") })

        if (actividad.isEmpty()) {
            Text(
                text = "Todavía no registraste ningún gasto.",
                modifier = Modifier.padding(24.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                items(actividad, key = { it.gasto.id }) { item ->
                    Column {
                        Row(
                            modifier = Modifier.padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val estilo = estiloGrupo(item.iconoGrupo)
                            Icon(
                                imageVector = estilo.icono,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = item.nombreGrupo,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                        GastoItem(gasto = item.gasto)
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
