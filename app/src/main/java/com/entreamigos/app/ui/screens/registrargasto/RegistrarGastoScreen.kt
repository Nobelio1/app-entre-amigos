package com.entreamigos.app.ui.screens.registrargasto

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.entreamigos.app.data.local.entity.CategoriaGasto
import com.entreamigos.app.data.repository.GastoRepository
import com.entreamigos.app.data.repository.GrupoRepository
import com.entreamigos.app.notification.NotificationHelper
import com.entreamigos.app.ui.components.FormFieldWithError
import com.entreamigos.app.ui.components.iconoParaCategoria
import com.entreamigos.app.ui.components.nombreCategoria
import com.entreamigos.app.viewmodel.RegistrarGastoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrarGastoScreen(
    grupoId: Long,
    grupoRepository: GrupoRepository,
    gastoRepository: GastoRepository,
    onGastoRegistrado: () -> Unit,
    onCancelar: () -> Unit
) {
    val viewModel: RegistrarGastoViewModel = viewModel(
        factory = RegistrarGastoViewModel.Factory(grupoId, grupoRepository, gastoRepository)
    )
    val miembros by viewModel.miembros.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(miembros) { viewModel.inicializarConMiembros(miembros) }

    val lanzadorPermisoNotificaciones = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {  }

    LaunchedEffect(uiState.gastoRegistrado) {
        if (uiState.gastoRegistrado) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !NotificationHelper.tienePermiso(context)) {
                lanzadorPermisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            NotificationHelper.notificar(
                context,
                titulo = "Gasto registrado",
                mensaje = "\"${uiState.descripcion}\" se agregó y se dividió entre ${uiState.participantesSeleccionados.size} persona(s)."
            )
            onGastoRegistrado()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Registrar gasto") },
            navigationIcon = {
                IconButton(onClick = onCancelar) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                FormFieldWithError(
                    valor = uiState.descripcion,
                    onValueChange = viewModel::actualizarDescripcion,
                    label = "Descripción (ej. Cena en restaurante)",
                    error = uiState.errorDescripcion
                )
            }
            item {
                FormFieldWithError(
                    valor = uiState.monto,
                    onValueChange = viewModel::actualizarMonto,
                    label = "Monto (S/)",
                    error = uiState.errorMonto,
                    keyboardType = KeyboardType.Decimal
                )
            }

            item {
                Text("Categoría", style = MaterialTheme.typography.titleMedium)
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(CategoriaGasto.values()) { categoria ->
                        FilterChip(
                            selected = uiState.categoria == categoria,
                            onClick = { viewModel.actualizarCategoria(categoria) },
                            label = { Text(nombreCategoria(categoria)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = iconoParaCategoria(categoria),
                                    contentDescription = null,
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                            }
                        )
                    }
                }
            }

            item {
                Text("¿Quién pagó?", style = MaterialTheme.typography.titleMedium)
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(miembros, key = { it.id }) { miembro ->
                        AssistChip(
                            onClick = { viewModel.actualizarPagadoPor(miembro.id) },
                            label = { Text(miembro.nombre) },
                            colors = if (uiState.pagadoPorId == miembro.id) {
                                androidx.compose.material3.AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            } else {
                                androidx.compose.material3.AssistChipDefaults.assistChipColors()
                            }
                        )
                    }
                }
            }

            item {
                Text("Dividir entre", style = MaterialTheme.typography.titleMedium)
                if (uiState.errorParticipantes != null) {
                    Text(uiState.errorParticipantes!!, color = MaterialTheme.colorScheme.error)
                }
            }
            items(miembros, key = { it.id }) { miembro ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = miembro.id in uiState.participantesSeleccionados,
                        onCheckedChange = { viewModel.alternarParticipante(miembro.id) }
                    )
                    Text(miembro.nombre)
                }
            }

            item {
                Button(
                    onClick = viewModel::guardarGasto,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.guardando
                ) {
                    Text(if (uiState.guardando) "Guardando..." else "Guardar gasto")
                }
            }
        }
    }
}
