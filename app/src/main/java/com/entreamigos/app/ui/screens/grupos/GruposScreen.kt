package com.entreamigos.app.ui.screens.grupos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.entreamigos.app.data.model.Resource
import com.entreamigos.app.data.repository.GrupoRepository
import com.entreamigos.app.data.repository.SincronizacionRepository
import com.entreamigos.app.ui.components.BannerConexion
import com.entreamigos.app.ui.components.ContenedorRefrescable
import com.entreamigos.app.ui.components.GroupCard
import com.entreamigos.app.ui.components.PantallaCargando
import com.entreamigos.app.ui.components.PantallaError
import com.entreamigos.app.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GruposScreen(
    grupoRepository: GrupoRepository,
    sincronizacionRepository: SincronizacionRepository,
    onGrupoClick: (Long) -> Unit
) {
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory(grupoRepository, sincronizacionRepository))
    val grupos by viewModel.grupos.collectAsStateWithLifecycle()
    val conexion by viewModel.conexion.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Mis grupos") })
        BannerConexion(estado = conexion, onReintentar = viewModel::refrescar)

        ContenedorRefrescable(
            refrescando = conexion.sincronizando,
            onRefrescar = viewModel::refrescar,
            modifier = Modifier.fillMaxSize()
        ) {
            when (val estado = grupos) {
                Resource.Loading -> PantallaCargando()
                is Resource.Error -> PantallaError(estado.mensaje, onReintentar = viewModel::refrescar)
                is Resource.Success -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (estado.data.isEmpty()) {
                        item {
                            Text(
                                text = "Todavía no tienes grupos.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    items(estado.data, key = { it.grupoId }) { grupo ->
                        GroupCard(resumen = grupo, onClick = { onGrupoClick(grupo.grupoId) })
                    }
                }
            }
        }
    }
}
