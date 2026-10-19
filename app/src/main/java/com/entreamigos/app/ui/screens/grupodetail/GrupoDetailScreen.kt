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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.entreamigos.app.data.model.Resource
import com.entreamigos.app.data.repository.GastoRepository
import com.entreamigos.app.data.repository.GrupoRepository
import com.entreamigos.app.data.repository.SincronizacionRepository
import com.entreamigos.app.ui.components.BannerConexion
import com.entreamigos.app.ui.components.ContenedorRefrescable
import com.entreamigos.app.ui.components.PantallaCargando
import com.entreamigos.app.ui.components.PantallaError
import com.entreamigos.app.ui.theme.MoradoGrupo
import com.entreamigos.app.viewmodel.GrupoDetailUiState
import com.entreamigos.app.viewmodel.GrupoDetailViewModel

private val TITULOS_TABS = listOf("Resumen", "Gastos", "Saldos", "Estadísticas")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrupoDetailScreen(
    grupoId: Long,
    grupoRepository: GrupoRepository,
    gastoRepository: GastoRepository,
    sincronizacionRepository: SincronizacionRepository,
    onVolver: () -> Unit,
    onRegistrarGastoClick: () -> Unit
) {
    val viewModel: GrupoDetailViewModel = viewModel(
        factory = GrupoDetailViewModel.Factory(grupoId, grupoRepository, gastoRepository, sincronizacionRepository)
    )
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val conexion by viewModel.conexion.collectAsStateWithLifecycle()
    var tabSeleccionada by rememberSaveable { mutableIntStateOf(0) }
    val uiState = (estado as? Resource.Success)?.data

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text(uiState?.grupo?.nombre ?: "Grupo", color = Color.White)
                    if (uiState != null) {
                        Text(
                            "${uiState.miembros.size} miembros",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onVolver) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MoradoGrupo)
        )
        BannerConexion(estado = conexion, onReintentar = viewModel::refrescar)

        when (val actual = estado) {
            Resource.Loading -> PantallaCargando()
            is Resource.Error -> PantallaError(actual.mensaje, onReintentar = onVolver)
            is Resource.Success -> {
                TabRow(selectedTabIndex = tabSeleccionada) {
                    TITULOS_TABS.forEachIndexed { index, titulo ->
                        Tab(
                            selected = tabSeleccionada == index,
                            onClick = { tabSeleccionada = index },
                            text = { Text(titulo) }
                        )
                    }
                }
                ContenedorRefrescable(
                    refrescando = conexion.sincronizando,
                    onRefrescar = viewModel::refrescar,
                    modifier = Modifier.fillMaxSize()
                ) {
                    ContenidoTab(tabSeleccionada, actual.data, viewModel, onRegistrarGastoClick)
                }
            }
        }
    }
}

@Composable
private fun ContenidoTab(
    tab: Int,
    uiState: GrupoDetailUiState,
    viewModel: GrupoDetailViewModel,
    onRegistrarGastoClick: () -> Unit
) {
    when (tab) {
        0 -> ResumenTab(uiState = uiState, onRegistrarGastoClick = onRegistrarGastoClick)
        1 -> GastosTab(uiState = uiState, onRegistrarGastoClick = onRegistrarGastoClick)
        2 -> SaldosTab(uiState = uiState, onLiquidar = viewModel::liquidarDeuda)
        3 -> EstadisticasTab(uiState = uiState)
    }
}
