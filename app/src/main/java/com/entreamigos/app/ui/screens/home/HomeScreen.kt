package com.entreamigos.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.WavingHand
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.entreamigos.app.data.model.Resource
import com.entreamigos.app.data.repository.GastoRepository
import com.entreamigos.app.data.repository.GrupoRepository
import com.entreamigos.app.data.repository.SincronizacionRepository
import com.entreamigos.app.ui.components.AccionSeccion
import com.entreamigos.app.ui.components.BannerConexion
import com.entreamigos.app.ui.components.ContenedorRefrescable
import com.entreamigos.app.ui.components.EncabezadoSeccion
import com.entreamigos.app.ui.components.GastoRecienteItem
import com.entreamigos.app.ui.components.GroupCard
import com.entreamigos.app.ui.components.IconosBarraDeEstado
import com.entreamigos.app.ui.components.PantallaCargando
import com.entreamigos.app.ui.components.PantallaError
import com.entreamigos.app.ui.components.ResumenGeneralRow
import com.entreamigos.app.ui.components.TarjetaEntreAmigos
import com.entreamigos.app.ui.theme.RojoBadge
import com.entreamigos.app.ui.theme.VerdeInicio
import com.entreamigos.app.ui.theme.VerdeMenta
import com.entreamigos.app.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

private val AmarilloSaludo = Color(0xFFF6C343)

@Composable
fun HomeScreen(
    grupoRepository: GrupoRepository,
    gastoRepository: GastoRepository,
    sincronizacionRepository: SincronizacionRepository,
    onGrupoClick: (Long) -> Unit,
    onCrearGrupoClick: () -> Unit,
    onVerTodosGastosClick: () -> Unit,
    onNotificacionesClick: () -> Unit,
    onPerfilClick: () -> Unit
) {
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(grupoRepository, gastoRepository, sincronizacionRepository)
    )
    val estadoGrupos by viewModel.grupos.collectAsStateWithLifecycle()
    val resumen by viewModel.resumenGeneral.collectAsStateWithLifecycle()
    val actividad by viewModel.actividad.collectAsStateWithLifecycle()
    val conexion by viewModel.conexion.collectAsStateWithLifecycle()
    val nombreUsuario by viewModel.nombreUsuario.collectAsStateWithLifecycle()
    val noVistas by viewModel.notificacionesNoVistas.collectAsStateWithLifecycle()

    IconosBarraDeEstado(claros = true)
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    fun avisoProximamente(texto: String) {
        scope.launch {
            drawerState.close()
            snackbar.showSnackbar(texto)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            MenuLateral(
                nombreUsuario = nombreUsuario,
                onPerfil = {
                    scope.launch { drawerState.close() }
                    onPerfilClick()
                },
                onConfiguracion = { avisoProximamente("Configuración estará disponible próximamente") },
                onCerrarSesion = { avisoProximamente("El inicio de sesión llega en la Fase 3") }
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize().background(VerdeInicio)) {
            Column(modifier = Modifier.fillMaxSize()) {
                HeaderInicio(
                    nombreUsuario = nombreUsuario,
                    notificacionesNoVistas = noVistas,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onNotificacionesClick = {
                        viewModel.marcarNotificacionesVistas()
                        onNotificacionesClick()
                    }
                )

                // Hoja clara que "sube" sobre el header verde, como en el mockup
                Surface(
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column {
                        BannerConexion(estado = conexion, onReintentar = viewModel::refrescar)
                        when (val estado = estadoGrupos) {
                            Resource.Loading -> PantallaCargando()
                            is Resource.Error -> PantallaError(estado.mensaje, onReintentar = viewModel::refrescar)
                            is Resource.Success -> ContenedorRefrescable(
                                refrescando = conexion.sincronizando,
                                onRefrescar = viewModel::refrescar,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    item(key = "titulo-grupos") {
                                        EncabezadoSeccion("Mis grupos") {
                                            AccionSeccion(
                                                texto = "Crear grupo",
                                                color = MaterialTheme.colorScheme.primary,
                                                icono = Icons.Outlined.AddCircleOutline,
                                                onClick = onCrearGrupoClick
                                            )
                                        }
                                    }
                                    if (estado.data.isEmpty()) {
                                        item(key = "grupos-vacio") {
                                            EstadoVacio("Todavía no tienes grupos. Crea el primero para empezar a registrar gastos compartidos.")
                                        }
                                    }
                                    // Prefijo en la key: este LazyColumn mezcla grupos con otras filas
                                    items(estado.data, key = { "grupo-${it.grupoId}" }) { grupo ->
                                        GroupCard(resumen = grupo, onClick = { onGrupoClick(grupo.grupoId) })
                                    }

                                    item(key = "titulo-resumen") {
                                        EncabezadoSeccion("Resumen general", modifier = Modifier.padding(top = 8.dp))
                                    }
                                    item(key = "resumen") { ResumenGeneralRow(resumen = resumen) }

                                    item(key = "titulo-recientes") {
                                        EncabezadoSeccion("Gastos recientes", modifier = Modifier.padding(top = 8.dp)) {
                                            AccionSeccion("Ver todos", MaterialTheme.colorScheme.primary, onVerTodosGastosClick)
                                        }
                                    }
                                    item(key = "recientes") {
                                        if (actividad.isEmpty()) {
                                            EstadoVacio("Cuando registres un gasto en alguno de tus grupos, aparecerá aquí.")
                                        } else {
                                            TarjetaEntreAmigos(modifier = Modifier.fillMaxWidth()) {
                                                actividad.take(3).forEachIndexed { indice, item ->
                                                    if (indice > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                                    GastoRecienteItem(item = item)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun HeaderInicio(
    nombreUsuario: String?,
    notificacionesNoVistas: Int,
    onMenuClick: () -> Unit,
    onNotificacionesClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(bottom = 20.dp)) {
        Box(modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp)) {
            IconButton(onClick = onMenuClick, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.Filled.Menu, contentDescription = "Abrir menú", tint = Color.White)
            }
            Row(
                modifier = Modifier.align(Alignment.Center).semantics(mergeDescendants = true) { heading() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Groups, contentDescription = null, tint = VerdeMenta, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "EntreAmigos",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
            val descripcionCampana = when (notificacionesNoVistas) {
                0 -> "Notificaciones"
                1 -> "Notificaciones, 1 gasto nuevo"
                else -> "Notificaciones, $notificacionesNoVistas gastos nuevos"
            }
            IconButton(
                onClick = onNotificacionesClick,
                modifier = Modifier.align(Alignment.CenterEnd).semantics { contentDescription = descripcionCampana }
            ) {
                BadgedBox(
                    badge = {
                        if (notificacionesNoVistas > 0) {
                            Badge(containerColor = RojoBadge, contentColor = Color.White) {
                                Text(if (notificacionesNoVistas > 9) "9+" else notificacionesNoVistas.toString())
                            }
                        }
                    }
                ) {
                    Icon(Icons.Outlined.Notifications, contentDescription = null, tint = Color.White)
                }
            }
        }

        Row(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (nombreUsuario != null) "¡Hola, $nombreUsuario!" else "¡Hola!",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge
            )
            Icon(
                Icons.Filled.WavingHand,
                contentDescription = null,
                tint = AmarilloSaludo,
                modifier = Modifier.padding(start = 8.dp).size(24.dp)
            )
        }
        Text(
            text = "Aquí tienes un resumen de tus grupos",
            color = Color.White.copy(alpha = 0.72f),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 2.dp)
        )
    }
}

/**
 * Menú lateral del ícono hamburguesa. Perfil navega; Configuración y Cerrar sesión son
 * marcadores de posición hasta las fases siguientes (no hay autenticación todavía).
 */
@Composable
private fun MenuLateral(
    nombreUsuario: String?,
    onPerfil: () -> Unit,
    onConfiguracion: () -> Unit,
    onCerrarSesion: () -> Unit
) {
    // windowInsets = 0: el encabezado verde se extiende detrás de la barra de estado
    // (lo compensa su statusBarsPadding), igual que el header de Home.
    ModalDrawerSheet(drawerContainerColor = MaterialTheme.colorScheme.surface, windowInsets = WindowInsets(0)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(VerdeInicio)
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            Icon(Icons.Filled.Groups, contentDescription = null, tint = VerdeMenta, modifier = Modifier.size(36.dp))
            Spacer(Modifier.height(12.dp))
            Text("EntreAmigos", color = Color.White, style = MaterialTheme.typography.titleMedium)
            Text(
                nombreUsuario ?: "Gastos compartidos sin complicaciones",
                color = Color.White.copy(alpha = 0.72f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(Modifier.height(12.dp))
        val modificador = Modifier.padding(horizontal = 12.dp)
        NavigationDrawerItem(
            label = { Text("Perfil") },
            icon = { Icon(Icons.Outlined.Person, contentDescription = null) },
            selected = false,
            onClick = onPerfil,
            modifier = modificador
        )
        NavigationDrawerItem(
            label = { Text("Configuración") },
            icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
            selected = false,
            onClick = onConfiguracion,
            modifier = modificador
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 28.dp))
        NavigationDrawerItem(
            label = { Text("Cerrar sesión") },
            icon = { Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null) },
            selected = false,
            onClick = onCerrarSesion,
            modifier = modificador
        )
    }
}

@Composable
private fun EstadoVacio(texto: String) {
    TarjetaEntreAmigos(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = texto,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )
    }
}
