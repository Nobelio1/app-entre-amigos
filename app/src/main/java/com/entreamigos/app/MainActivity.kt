package com.entreamigos.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.entreamigos.app.ui.screens.login.LoginScreen
import androidx.compose.ui.Modifier
import com.entreamigos.app.ui.navigation.EntreAmigosNavGraph
import com.entreamigos.app.ui.theme.EntreAmigosTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EntreAmigosRoot()
        }
    }
}

@Composable
private fun EntreAmigosRoot() {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as EntreAmigosApp
    val sesion by app.container.authRepository.sesion.collectAsStateWithLifecycle()

    EntreAmigosTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            val actual = sesion
            if (actual == null) {
                LoginScreen(authRepository = app.container.authRepository)
            } else {
                // Al cambiar de cuenta se descarta el back stack anterior
                key(actual.usuarioId) {
                    EntreAmigosNavGraph(
                        grupoRepository = app.container.grupoRepository,
                        gastoRepository = app.container.gastoRepository,
                        sincronizacionRepository = app.container.sincronizacionRepository,
                        authRepository = app.container.authRepository
                    )
                }
            }
        }
    }
}
