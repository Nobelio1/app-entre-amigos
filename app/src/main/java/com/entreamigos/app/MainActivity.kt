package com.entreamigos.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
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

    EntreAmigosTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            EntreAmigosNavGraph(
                grupoRepository = app.container.grupoRepository,
                gastoRepository = app.container.gastoRepository,
                sincronizacionRepository = app.container.sincronizacionRepository
            )
        }
    }
}
