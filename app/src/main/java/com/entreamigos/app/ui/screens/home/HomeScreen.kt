package com.entreamigos.app.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.entreamigos.app.data.repository.GastoRepository
import com.entreamigos.app.data.repository.GrupoRepository
import com.entreamigos.app.ui.components.GastoItem
import com.entreamigos.app.ui.components.GroupCard
import com.entreamigos.app.ui.components.ResumenGeneralRow
import com.entreamigos.app.ui.theme.VerdeInicio
import com.entreamigos.app.viewmodel.GastosGlobalViewModel
import com.entreamigos.app.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    grupoRepository: GrupoRepository,
    gastoRepository: GastoRepository,
    onGrupoClick: (Long) -> Unit,
    onCrearGrupoClick: () -> Unit,
    onVerTodosGastosClick: () -> Unit
) {
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory(grupoRepository))
    val grupos by viewModel.grupos.collectAsStateWithLifecycle()
    val resumen by viewModel.resumenGeneral.collectAsStateWithLifecycle()

    val gastosGlobalViewModel: GastosGlobalViewModel = viewModel(factory = GastosGlobalViewModel.Factory(gastoRepository))
    val actividad by gastosGlobalViewModel.actividad.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Icon(Icons.Filled.Groups, contentDescription = null, tint = Color.White)
                        Text(
                            text = "  EntreAmigos",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = VerdeInicio)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(text = "¡Hola!", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "Aquí tienes un resumen de tus grupos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(text = "Mis grupos", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = onCrearGrupoClick) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                        Text("Crear grupo")
                    }
                }
            }

            if (grupos.isEmpty()) {
                item {
                    Text(
                        text = "Todavía no tienes grupos. Crea el primero para empezar a registrar gastos compartidos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(grupos, key = { it.grupoId }) { grupo ->
                    GroupCard(resumen = grupo, onClick = { onGrupoClick(grupo.grupoId) })
                }
            }

            item {
                Text(text = "Resumen general", style = MaterialTheme.typography.titleMedium)
            }
            item {
                ResumenGeneralRow(resumen = resumen)
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Gastos recientes", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = onVerTodosGastosClick) { Text("Ver todos") }
                }
            }

            if (actividad.isEmpty()) {
                item {
                    Text(
                        text = "Cuando registres un gasto en alguno de tus grupos, aparecerá aquí.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(actividad.take(3), key = { it.gasto.id }) { item ->
                    GastoItem(gasto = item.gasto)
                }
            }
        }
    }
}
