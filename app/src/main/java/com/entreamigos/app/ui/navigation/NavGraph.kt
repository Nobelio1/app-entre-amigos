package com.entreamigos.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.entreamigos.app.data.repository.GastoRepository
import com.entreamigos.app.data.repository.GrupoRepository
import com.entreamigos.app.data.repository.SincronizacionRepository
import com.entreamigos.app.ui.screens.creargrupo.CrearGrupoScreen
import com.entreamigos.app.ui.screens.gastos.GastosGlobalScreen
import com.entreamigos.app.ui.screens.grupodetail.GrupoDetailScreen
import com.entreamigos.app.ui.screens.grupos.GruposScreen
import com.entreamigos.app.ui.screens.home.HomeScreen
import com.entreamigos.app.ui.screens.perfil.PerfilScreen
import com.entreamigos.app.ui.screens.registrargasto.RegistrarGastoScreen

@Composable
fun EntreAmigosNavGraph(
    grupoRepository: GrupoRepository,
    gastoRepository: GastoRepository,
    sincronizacionRepository: SincronizacionRepository,
    navController: NavHostController = rememberNavController()
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val rutaActual = backStackEntry?.destination?.route

    val mostrarBottomBar = rutaActual in setOf(
        Screen.Home.route, "grupos", "gastos", Screen.Perfil.route
    )

    Scaffold(
        contentWindowInsets = WindowInsets.navigationBars,
        bottomBar = {
            if (mostrarBottomBar) {
                EntreAmigosBottomBar(rutaActual = rutaActual) { destino ->
                    if (destino == BottomDestino.CREAR) {
                        navController.navigate(Screen.CrearGrupo.route)
                    } else {
                        navegarATab(navController, destino.ruta)
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = androidx.compose.ui.Modifier.padding(padding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    grupoRepository = grupoRepository,
                    gastoRepository = gastoRepository,
                    sincronizacionRepository = sincronizacionRepository,
                    onGrupoClick = { grupoId -> navController.navigate(Screen.GrupoDetail.crearRuta(grupoId)) },
                    onCrearGrupoClick = { navController.navigate(Screen.CrearGrupo.route) },
                    onVerTodosGastosClick = { navegarATab(navController, "gastos") },
                    onNotificacionesClick = { navegarATab(navController, "gastos") },
                    onPerfilClick = { navegarATab(navController, Screen.Perfil.route) }
                )
            }

            composable("grupos") {
                GruposScreen(
                    grupoRepository = grupoRepository,
                    gastoRepository = gastoRepository,
                    sincronizacionRepository = sincronizacionRepository,
                    onGrupoClick = { grupoId -> navController.navigate(Screen.GrupoDetail.crearRuta(grupoId)) }
                )
            }

            composable("gastos") {
                GastosGlobalScreen(gastoRepository = gastoRepository)
            }

            composable(Screen.Perfil.route) {
                PerfilScreen()
            }

            composable(Screen.CrearGrupo.route) {
                CrearGrupoScreen(
                    grupoRepository = grupoRepository,
                    onGrupoCreado = { grupoId ->
                        navController.navigate(Screen.GrupoDetail.crearRuta(grupoId)) {
                            popUpTo(Screen.Home.route)
                        }
                    },
                    onCancelar = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.GrupoDetail.route,
                arguments = listOf(navArgument("grupoId") { type = NavType.LongType })
            ) { backStack ->
                val grupoId = backStack.arguments?.getLong("grupoId") ?: return@composable
                GrupoDetailScreen(
                    grupoId = grupoId,
                    grupoRepository = grupoRepository,
                    gastoRepository = gastoRepository,
                    sincronizacionRepository = sincronizacionRepository,
                    onVolver = { navController.popBackStack() },
                    onRegistrarGastoClick = { navController.navigate(Screen.RegistrarGasto.crearRuta(grupoId)) }
                )
            }

            composable(
                route = Screen.RegistrarGasto.route,
                arguments = listOf(navArgument("grupoId") { type = NavType.LongType })
            ) { backStack ->
                val grupoId = backStack.arguments?.getLong("grupoId") ?: return@composable
                RegistrarGastoScreen(
                    grupoId = grupoId,
                    grupoRepository = grupoRepository,
                    gastoRepository = gastoRepository,
                    onGastoRegistrado = { navController.popBackStack() },
                    onCancelar = { navController.popBackStack() }
                )
            }
        }
    }
}

private fun navegarATab(navController: NavHostController, ruta: String) {
    navController.navigate(ruta) {
        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
