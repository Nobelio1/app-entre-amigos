package com.entreamigos.app.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object CrearGrupo : Screen("crear_grupo")
    data object Perfil : Screen("perfil")

    data object GrupoDetail : Screen("grupo/{grupoId}") {
        fun crearRuta(grupoId: Long) = "grupo/$grupoId"
    }

    data object RegistrarGasto : Screen("grupo/{grupoId}/registrar_gasto") {
        fun crearRuta(grupoId: Long) = "grupo/$grupoId/registrar_gasto"
    }

    data object EditarGasto : Screen("grupo/{grupoId}/gasto/{gastoId}/editar") {
        fun crearRuta(grupoId: Long, gastoId: Long) = "grupo/$grupoId/gasto/$gastoId/editar"
    }
}
