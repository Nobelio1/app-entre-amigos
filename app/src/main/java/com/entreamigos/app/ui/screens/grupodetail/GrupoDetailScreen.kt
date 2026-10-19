package com.entreamigos.app.ui.screens.grupodetail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.entreamigos.app.data.model.Resource
import com.entreamigos.app.data.repository.GastoRepository
import com.entreamigos.app.data.repository.GrupoRepository
import com.entreamigos.app.data.repository.SincronizacionRepository
import com.entreamigos.app.ui.components.BannerConexion
import com.entreamigos.app.ui.components.ContenedorRefrescable
import com.entreamigos.app.ui.components.IconoCircular
import com.entreamigos.app.ui.components.IconosBarraDeEstado
import com.entreamigos.app.ui.components.PantallaCargando
import com.entreamigos.app.ui.components.PantallaError
import com.entreamigos.app.ui.components.estiloGrupo
import com.entreamigos.app.ui.theme.MoradoGrupo
import com.entreamigos.app.util.compartirResumenGrupo
import com.entreamigos.app.viewmodel.GrupoDetailUiState
import com.entreamigos.app.viewmodel.GrupoDetailViewModel

/** Pestañas con los íconos del mockup: gráfico circular, lista, personas y barras. */
private enum class PestanaGrupo(val titulo: String, val icono: ImageVector, val iconoActivo: ImageVector) {
    RESUMEN("Resumen", Icons.Outlined.PieChart, Icons.Filled.PieChart),
    GASTOS("Gastos", Icons.AutoMirrored.Outlined.ReceiptLong, Icons.AutoMirrored.Outlined.ReceiptLong),
    SALDOS("Saldos", Icons.Outlined.People, Icons.Outlined.People),
    ESTADISTICAS("Estadísticas", Icons.Outlined.BarChart, Icons.Outlined.BarChart)
}

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
    var pestana by rememberSaveable { mutableIntStateOf(0) }
    val uiState = (estado as? Resource.Success)?.data

    IconosBarraDeEstado(claros = true)

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.background(MoradoGrupo)) {
            HeaderGrupo(uiState = uiState, onVolver = onVolver, onSincronizar = viewModel::refrescar)
            if (uiState != null) {
                TarjetaPestanas(seleccionada = pestana, onSeleccionar = { pestana = it })
            } else {
                Spacer(Modifier.height(16.dp))
            }
        }
        BannerConexion(estado = conexion, onReintentar = viewModel::refrescar)

        when (val actual = estado) {
            Resource.Loading -> PantallaCargando()
            is Resource.Error -> PantallaError(actual.mensaje, onReintentar = onVolver)
            is Resource.Success -> ContenedorRefrescable(
                refrescando = conexion.sincronizando,
                onRefrescar = viewModel::refrescar,
                modifier = Modifier.fillMaxSize()
            ) {
                when (PestanaGrupo.entries[pestana]) {
                    PestanaGrupo.RESUMEN -> ResumenTab(
                        uiState = actual.data,
                        onRegistrarGastoClick = onRegistrarGastoClick,
                        onVerTodosLosGastos = { pestana = PestanaGrupo.GASTOS.ordinal },
                        onIrASaldos = { pestana = PestanaGrupo.SALDOS.ordinal }
                    )
                    PestanaGrupo.GASTOS -> GastosTab(uiState = actual.data, onRegistrarGastoClick = onRegistrarGastoClick)
                    PestanaGrupo.SALDOS -> SaldosTab(uiState = actual.data, onLiquidar = viewModel::liquidarDeuda)
                    PestanaGrupo.ESTADISTICAS -> EstadisticasTab(uiState = actual.data)
                }
            }
        }
    }
}

@Composable
private fun HeaderGrupo(uiState: GrupoDetailUiState?, onVolver: () -> Unit, onSincronizar: () -> Unit) {
    val context = LocalContext.current
    var menuAbierto by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onVolver) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
        }
        val grupo = uiState?.grupo
        if (grupo != null) {
            // Avatar del grupo: círculo blanco con borde suave, como la foto del mockup
            Surface(shape = CircleShape, color = Color.White, border = BorderStroke(2.dp, Color.White.copy(alpha = 0.6f))) {
                IconoCircular(estilo = estiloGrupo(grupo.icono), tamano = 48.dp, relleno = false)
            }
            Spacer(Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = grupo?.nombre ?: "Grupo",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (uiState != null) {
                Text(
                    text = "${uiState.miembros.size} miembros",
                    color = Color.White.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        Box {
            IconButton(onClick = { menuAbierto = true }, enabled = uiState != null) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Más opciones", tint = Color.White)
            }
            DropdownMenu(expanded = menuAbierto, onDismissRequest = { menuAbierto = false }) {
                DropdownMenuItem(
                    text = { Text("Compartir resumen") },
                    leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                    onClick = {
                        menuAbierto = false
                        uiState?.let { compartirResumenGrupo(context, it) }
                    }
                )
                DropdownMenuItem(
                    text = { Text("Sincronizar ahora") },
                    leadingIcon = { Icon(Icons.Filled.Sync, contentDescription = null) },
                    onClick = {
                        menuAbierto = false
                        onSincronizar()
                    }
                )
            }
        }
    }
}

/** Tarjeta blanca de pestañas que se superpone al header morado (ícono sobre texto). */
@Composable
private fun TarjetaPestanas(seleccionada: Int, onSeleccionar: (Int) -> Unit) {
    val colorActivo = MaterialTheme.colorScheme.secondary
    Surface(
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
    ) {
        TabRow(
            selectedTabIndex = seleccionada,
            containerColor = Color.Transparent,
            contentColor = colorActivo,
            divider = {},
            indicator = { posiciones ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(posiciones[seleccionada]),
                    color = colorActivo
                )
            }
        ) {
            PestanaGrupo.entries.forEachIndexed { indice, pestana ->
                val activa = indice == seleccionada
                Tab(
                    selected = activa,
                    onClick = { onSeleccionar(indice) },
                    selectedContentColor = colorActivo,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    icon = {
                        Icon(
                            if (activa) pestana.iconoActivo else pestana.icono,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    text = {
                        Text(
                            pestana.titulo,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                )
            }
        }
    }
}
