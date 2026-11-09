package com.entreamigos.app.ui.screens.grupos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.entreamigos.app.data.repository.AuthRepository
import com.entreamigos.app.ui.components.AvatarInicial
import com.entreamigos.app.viewmodel.UnirseGrupoViewModel

/**
 * Unirse a un grupo creado en otro dispositivo: 1) código de invitación, 2) elegir con qué
 * integrante del grupo te identificas (o entrar como integrante nuevo).
 */
@Composable
fun UnirseGrupoDialog(
    authRepository: AuthRepository,
    /** Distinto en cada apertura para que el formulario empiece vacío. */
    instancia: Int,
    onCerrar: () -> Unit,
    onUnido: (Long) -> Unit
) {
    val viewModel: UnirseGrupoViewModel = viewModel(
        key = "unirse-grupo-$instancia",
        factory = UnirseGrupoViewModel.Factory(authRepository)
    )
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(estado.grupoUnidoId) {
        estado.grupoUnidoId?.let(onUnido)
    }

    val invitacion = estado.invitacion
    AlertDialog(
        onDismissRequest = { if (!estado.cargando) onCerrar() },
        title = { Text(invitacion?.nombre ?: "Unirse a un grupo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (invitacion == null) {
                    Text(
                        "Pide a alguien del grupo el código de invitación (menú ⋮ del grupo → Invitar).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = estado.codigo,
                        onValueChange = viewModel::actualizarCodigo,
                        label = { Text("Código") },
                        singleLine = true,
                        isError = estado.error != null,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Search
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text("¿Quién eres en este grupo?", style = MaterialTheme.typography.bodyMedium)
                    Column(
                        modifier = Modifier.heightIn(max = 280.dp).verticalScroll(rememberScrollState())
                    ) {
                        invitacion.miembros.filter { !it.vinculado }.forEach { miembro ->
                            OpcionMiembro(
                                texto = miembro.nombre,
                                colorHex = miembro.colorAvatarHex,
                                seleccionado = estado.miembroElegido == miembro.id,
                                onClick = { viewModel.elegirMiembro(miembro.id) }
                            )
                        }
                        OpcionMiembro(
                            texto = "Soy nuevo, agrégame al grupo",
                            colorHex = null,
                            seleccionado = estado.miembroElegido == null,
                            onClick = { viewModel.elegirMiembro(null) }
                        )
                    }
                    val vinculados = invitacion.miembros.filter { it.vinculado }
                    if (vinculados.isNotEmpty()) {
                        Text(
                            "Ya están en el grupo: " + vinculados.joinToString { it.nombre },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                estado.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (invitacion == null) viewModel.buscar() else viewModel.unirse() },
                enabled = !estado.cargando
            ) {
                if (estado.cargando) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (invitacion == null) "Buscar" else "Unirme")
            }
        },
        dismissButton = {
            TextButton(
                onClick = { if (invitacion == null) onCerrar() else viewModel.volver() },
                enabled = !estado.cargando
            ) {
                Text(if (invitacion == null) "Cancelar" else "Atrás")
            }
        }
    )
}

@Composable
private fun OpcionMiembro(texto: String, colorHex: String?, seleccionado: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = seleccionado, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = seleccionado, onClick = null)
        Spacer(Modifier.width(8.dp))
        if (colorHex != null) {
            AvatarInicial(nombre = texto, colorHex = colorHex, tamano = 28.dp)
            Spacer(Modifier.width(8.dp))
        }
        Text(texto, style = MaterialTheme.typography.bodyLarge)
    }
}
