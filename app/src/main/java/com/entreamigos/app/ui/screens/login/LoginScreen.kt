package com.entreamigos.app.ui.screens.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.entreamigos.app.R
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.entreamigos.app.data.repository.AuthRepository
import com.entreamigos.app.ui.components.IconosBarraDeEstado
import com.entreamigos.app.ui.theme.VerdeAccion
import com.entreamigos.app.ui.theme.VerdeInicio
import com.entreamigos.app.ui.theme.VerdeMenta
import com.entreamigos.app.viewmodel.LoginViewModel

@Composable
fun LoginScreen(authRepository: AuthRepository) {
    val viewModel: LoginViewModel = viewModel(factory = LoginViewModel.Factory(authRepository))
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    var verPassword by rememberSaveable { mutableStateOf(false) }
    val contexto = LocalContext.current

    IconosBarraDeEstado(claros = true)

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(VerdeInicio)) {
        val altoPantalla = maxHeight
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Filled.Groups, contentDescription = null, tint = VerdeMenta, modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(12.dp))
                Text("EntreAmigos", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Gastos compartidos sin complicaciones",
                    color = Color.White.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Surface(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxWidth().heightIn(min = altoPantalla * 0.7f)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        if (estado.modoRegistro) "Crea tu cuenta" else "Inicia sesión",
                        style = MaterialTheme.typography.titleLarge
                    )

                    if (estado.modoRegistro) {
                        CampoTexto(
                            valor = estado.nombre,
                            onCambio = viewModel::actualizarNombre,
                            etiqueta = "Nombre",
                            error = estado.errorNombre,
                            opciones = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next)
                        )
                    }
                    CampoTexto(
                        valor = estado.email,
                        onCambio = viewModel::actualizarEmail,
                        etiqueta = "Email",
                        error = estado.errorEmail,
                        opciones = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
                    )
                    CampoTexto(
                        valor = estado.password,
                        onCambio = viewModel::actualizarPassword,
                        etiqueta = "Contraseña",
                        error = estado.errorPassword,
                        opciones = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        acciones = KeyboardActions(onDone = { viewModel.enviar() }),
                        transformacion = if (verPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        icono = {
                            IconButton(onClick = { verPassword = !verPassword }) {
                                Icon(
                                    if (verPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = if (verPassword) "Ocultar contraseña" else "Mostrar contraseña"
                                )
                            }
                        }
                    )

                    estado.errorGeneral?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }

                    Button(
                        onClick = viewModel::enviar,
                        enabled = !estado.ocupado,
                        colors = ButtonDefaults.buttonColors(containerColor = VerdeAccion, contentColor = Color.White),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        if (estado.cargando) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                        } else {
                            Text(if (estado.modoRegistro) "Crear cuenta" else "Entrar")
                        }
                    }

                    if (viewModel.googleDisponible) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            HorizontalDivider(modifier = Modifier.weight(1f))
                            Text(
                                "  o  ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f))
                        }
                        OutlinedButton(
                            onClick = { viewModel.continuarConGoogle(contexto) },
                            enabled = !estado.ocupado,
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            if (estado.cargandoGoogle) {
                                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                            } else {
                                Icon(
                                    painterResource(R.drawable.ic_google),
                                    contentDescription = null,
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text("Continuar con Google")
                            }
                        }
                    }

                    TextButton(onClick = viewModel::alternarModo, enabled = !estado.ocupado, modifier = Modifier.fillMaxWidth()) {
                        Text(if (estado.modoRegistro) "¿Ya tienes cuenta? Inicia sesión" else "¿No tienes cuenta? Regístrate")
                    }
                }
            }
        }
    }
}

@Composable
private fun CampoTexto(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    error: String?,
    opciones: KeyboardOptions,
    acciones: KeyboardActions = KeyboardActions.Default,
    transformacion: VisualTransformation = VisualTransformation.None,
    icono: (@Composable () -> Unit)? = null
) {
    Column {
        OutlinedTextField(
            value = valor,
            onValueChange = onCambio,
            label = { Text(etiqueta) },
            isError = error != null,
            singleLine = true,
            keyboardOptions = opciones,
            keyboardActions = acciones,
            visualTransformation = transformacion,
            trailingIcon = icono,
            modifier = Modifier.fillMaxWidth()
        )
        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
        }
    }
}
