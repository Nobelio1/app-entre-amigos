package com.entreamigos.app.ui.screens.creargrupo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.entreamigos.app.data.repository.GrupoRepository
import com.entreamigos.app.ui.components.FormFieldWithError
import com.entreamigos.app.viewmodel.CrearGrupoViewModel
import com.entreamigos.app.ui.components.IconoCircular
import com.entreamigos.app.ui.components.estiloGrupo
import com.entreamigos.app.viewmodel.ICONOS_GRUPO

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearGrupoScreen(
    grupoRepository: GrupoRepository,
    onGrupoCreado: (Long) -> Unit,
    onCancelar: () -> Unit
) {
    val viewModel: CrearGrupoViewModel = viewModel(factory = CrearGrupoViewModel.Factory(grupoRepository))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.grupoCreadoId) {
        uiState.grupoCreadoId?.let(onGrupoCreado)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Crear grupo") },
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
                    valor = uiState.nombreGrupo,
                    onValueChange = viewModel::actualizarNombreGrupo,
                    label = "Nombre del grupo (ej. Viaje a Punta Cana)",
                    error = uiState.errorNombreGrupo
                )
            }

            item {
                Text(text = "Ícono del grupo", style = MaterialTheme.typography.titleMedium)
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ICONOS_GRUPO) { icono ->
                        val seleccionado = icono == uiState.iconoSeleccionado
                        Card(
                            onClick = { viewModel.seleccionarIcono(icono) },
                            shape = CircleShape,
                            colors = CardDefaults.cardColors(
                                containerColor = if (seleccionado) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.semantics { selected = seleccionado }
                        ) {
                            IconoCircular(
                                estilo = estiloGrupo(icono),
                                relleno = seleccionado,
                                tamano = 48.dp,
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }
                }
            }

            item {
                Text(text = "Integrantes", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "El primer integrante eres tú.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            itemsIndexed(uiState.nombresMiembros) { index, nombre ->
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    FormFieldWithError(
                        valor = nombre,
                        onValueChange = { viewModel.actualizarMiembro(index, it) },
                        label = if (index == 0) "Tú" else "Integrante ${index + 1}",
                        error = null,
                        modifier = Modifier.weight(1f)
                    )
                    if (uiState.nombresMiembros.size > 2) {
                        IconButton(onClick = { viewModel.eliminarCampoMiembro(index) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Quitar integrante")
                        }
                    }
                }
            }

            if (uiState.errorMiembros != null) {
                item {
                    Text(text = uiState.errorMiembros!!, color = MaterialTheme.colorScheme.error)
                }
            }

            item {
                OutlinedButton(onClick = viewModel::agregarCampoMiembro, modifier = Modifier.fillMaxWidth()) {
                    Text("+ Agregar integrante")
                }
            }

            item {
                Button(
                    onClick = viewModel::crearGrupo,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.guardando
                ) {
                    Text(if (uiState.guardando) "Creando..." else "Crear grupo")
                }
            }
        }
    }
}
