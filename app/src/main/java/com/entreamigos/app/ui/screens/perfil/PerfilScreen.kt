package com.entreamigos.app.ui.screens.perfil

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.entreamigos.app.BuildConfig
import com.entreamigos.app.data.repository.AuthRepository
import com.entreamigos.app.ui.components.AvatarInicial
import com.entreamigos.app.ui.components.TarjetaEntreAmigos

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilScreen(
    authRepository: AuthRepository,
    onUnirseAGrupo: () -> Unit,
    onCerrarSesion: () -> Unit
) {
    val sesion by authRepository.sesion.collectAsStateWithLifecycle()
    var confirmarSalida by rememberSaveable { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Perfil") })

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            sesion?.let { usuario ->
                TarjetaEntreAmigos(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        AvatarInicial(nombre = usuario.nombre, colorHex = usuario.colorAvatarHex, tamano = 56.dp)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(usuario.nombre, style = MaterialTheme.typography.titleMedium)
                            Text(
                                usuario.email,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            OutlinedButton(onClick = onUnirseAGrupo, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.GroupAdd, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Unirme a un grupo con código")
            }

            OutlinedButton(onClick = { confirmarSalida = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(8.dp))
                Text("Cerrar sesión", color = MaterialTheme.colorScheme.error)
            }

            Text(
                "EntreAmigos ${BuildConfig.VERSION_NAME} · Gestión y control de gastos compartidos",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (confirmarSalida) {
        DialogoCerrarSesion(
            onConfirmar = {
                confirmarSalida = false
                onCerrarSesion()
            },
            onCancelar = { confirmarSalida = false }
        )
    }
}

@Composable
fun DialogoCerrarSesion(onConfirmar: () -> Unit, onCancelar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("¿Cerrar sesión?") },
        text = {
            Text("Se borrarán de este dispositivo tus grupos y los cambios que aún no se hayan sincronizado.")
        },
        confirmButton = {
            TextButton(onClick = onConfirmar) { Text("Cerrar sesión", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}
