package com.entreamigos.app.ui.screens.grupodetail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.entreamigos.app.ui.components.AccionSeccion
import com.entreamigos.app.ui.components.DeudaItem
import com.entreamigos.app.ui.components.EncabezadoSeccion
import com.entreamigos.app.ui.components.GastoItem
import com.entreamigos.app.ui.components.TarjetaEntreAmigos
import com.entreamigos.app.ui.components.colorDeBalance
import com.entreamigos.app.util.CurrencyFormatter
import com.entreamigos.app.viewmodel.GrupoDetailUiState
import kotlin.math.abs

private val VerdeBillete = Color(0xFF43A047)
private val FormaBoton = RoundedCornerShape(10.dp)

@Composable
fun ResumenTab(
    uiState: GrupoDetailUiState,
    onRegistrarGastoClick: () -> Unit,
    onVerTodosLosGastos: () -> Unit,
    onIrASaldos: () -> Unit
) {
    val usuarioId = uiState.miembros.firstOrNull { it.esUsuarioActual }?.id
    // En el Resumen solo importan las deudas en las que participa el usuario;
    // la lista completa del grupo está en la pestaña Saldos.
    val deudasDelUsuario = uiState.deudasQueLeDebenAlUsuario + uiState.deudasQueElUsuarioDebe
    val colorAcento = MaterialTheme.colorScheme.secondary

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "balance") { TarjetaBalance(balance = uiState.balanceUsuarioActual) }

        item(key = "titulo-deudas") { EncabezadoSeccion("¿Quién debe a quién?") }
        item(key = "deudas") {
            TarjetaEntreAmigos(modifier = Modifier.fillMaxWidth()) {
                if (deudasDelUsuario.isEmpty()) {
                    Text(
                        text = if (uiState.deudas.isEmpty()) "No hay deudas pendientes en este grupo."
                        else "No tienes deudas pendientes. Revisa Saldos para ver las del resto del grupo.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    deudasDelUsuario.forEachIndexed { indice, deuda ->
                        if (indice > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        DeudaItem(
                            deuda = deuda,
                            esDeudaHaciaElUsuario = deuda.acreedor.id == usuarioId,
                            onClick = onIrASaldos
                        )
                    }
                }
            }
        }

        item(key = "registrar") {
            Button(
                onClick = onRegistrarGastoClick,
                shape = FormaBoton,
                colors = ButtonDefaults.buttonColors(containerColor = colorAcento),
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Registrar nuevo gasto", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                Icon(Icons.Filled.Add, contentDescription = null)
            }
        }

        item(key = "titulo-actividad") {
            EncabezadoSeccion("Actividad reciente", modifier = Modifier.padding(top = 4.dp)) {
                AccionSeccion("Ver todo", colorAcento, onVerTodosLosGastos)
            }
        }
        item(key = "actividad") {
            TarjetaEntreAmigos(modifier = Modifier.fillMaxWidth()) {
                if (uiState.gastos.isEmpty()) {
                    Text(
                        "Aún no hay gastos registrados en este grupo.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    uiState.gastos.take(3).forEachIndexed { indice, gasto ->
                        if (indice > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        GastoItem(gasto = gasto)
                    }
                }
            }
        }

        item(key = "liquidar") {
            OutlinedButton(
                onClick = onIrASaldos,
                enabled = uiState.deudas.isNotEmpty(),
                shape = FormaBoton,
                border = BorderStroke(1.dp, if (uiState.deudas.isNotEmpty()) colorAcento else MaterialTheme.colorScheme.outlineVariant),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colorAcento),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(
                    "Liquidar deudas",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).padding(start = 24.dp)
                )
                Icon(Icons.Outlined.MonetizationOn, contentDescription = null)
            }
        }
    }
}

@Composable
private fun TarjetaBalance(balance: Double) {
    val color = colorDeBalance(balance)
    val sufijo = when {
        balance > 0.005 -> "te deben"
        balance < -0.005 -> "debes"
        else -> "estás al día"
    }
    val monto = (if (balance < -0.005) "- " else "+ ") + CurrencyFormatter.formatear(abs(balance))

    TarjetaEntreAmigos(
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics {
            contentDescription = "Tu balance en este grupo: $monto, $sufijo"
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Tu balance en este grupo",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                )
                Text(text = monto, style = MaterialTheme.typography.headlineSmall, color = color)
                Text(text = sufijo, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), color = color)
            }
            // Reemplazo vectorial de la ilustración de billetes del mockup
            Icon(
                Icons.Filled.Payments,
                contentDescription = null,
                tint = VerdeBillete,
                modifier = Modifier.size(72.dp)
            )
        }
    }
}
