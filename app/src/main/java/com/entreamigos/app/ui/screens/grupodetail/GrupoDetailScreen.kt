package com.entreamigos.app.ui.screens.grupodetail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.entreamigos.app.data.repository.GastoRepository
import com.entreamigos.app.data.repository.GrupoRepository
import com.entreamigos.app.ui.theme.MoradoGrupo
import com.entreamigos.app.viewmodel.GrupoDetailViewModel

private val TITULOS_TABS = listOf("Resumen", "Gastos", "Saldos", "Estadísticas")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrupoDetailScreen(
    grupoId: Long,
    grupoRepository: GrupoRepository,
    gastoRepository: GastoRepository,
    onVolver: () -> Unit,
    onRegistrarGastoClick: () -> Unit
) {
    val viewModel: GrupoDetailViewModel = viewModel(
        factory = GrupoDetailViewModel.Factory(grupoId, grupoRepository, gastoRepository)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var tabSeleccionada by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text(uiState.grupo?.nombre ?: "Grupo", color = Color.White)
                    Text(
                        "${uiState.miembros.size} miembros",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onVolver) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MoradoGrupo)
        )

        TabRow(selectedTabIndex = tabSeleccionada) {
            TITULOS_TABS.forEachIndexed { index, titulo ->
                Tab(
                    selected = tabSeleccionada == index,
                    onClick = { tabSeleccionada = index },
                    text = { Text(titulo) }
                )
            }
        }

        when (tabSeleccionada) {
            0 -> ResumenTab(uiState = uiState, onRegistrarGastoClick = onRegistrarGastoClick)
            1 -> GastosTab(uiState = uiState, onRegistrarGastoClick = onRegistrarGastoClick)
            2 -> SaldosTab(uiState = uiState, onLiquidar = viewModel::liquidarDeuda)
            3 -> EstadisticasTab(uiState = uiState)
        }
    }
}
