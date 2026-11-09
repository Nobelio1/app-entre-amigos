package com.entreamigos.app.util

import android.content.Context
import android.content.Intent
import com.entreamigos.app.viewmodel.GrupoDetailUiState

/** Comparte el código para que otra persona se una al grupo desde su propio dispositivo. */
fun compartirInvitacion(context: Context, nombreGrupo: String, codigo: String) {
    val texto = "Únete a \"$nombreGrupo\" en EntreAmigos para dividir gastos conmigo.\n" +
        "Abre la app → Perfil → Unirme a un grupo con código e ingresa: $codigo"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, texto)
    }
    context.startActivity(Intent.createChooser(intent, "Invitar a $nombreGrupo"))
}

fun compartirResumenGrupo(context: Context, uiState: GrupoDetailUiState) {
    val nombreGrupo = uiState.grupo?.nombre ?: "mi grupo"
    val balance = uiState.balanceUsuarioActual
    val lineaBalance = when {
        balance > 0.005 -> "Me deben ${CurrencyFormatter.formatear(balance)}"
        balance < -0.005 -> "Debo ${CurrencyFormatter.formatear(-balance)}"
        else -> "Estoy al día"
    }

    val detalleDeudas = uiState.deudas.joinToString(separator = "\n") { deuda ->
        "${deuda.deudor.nombre} le debe ${CurrencyFormatter.formatear(deuda.monto)} a ${deuda.acreedor.nombre}"
    }

    val texto = buildString {
        appendLine("Resumen de \"$nombreGrupo\" - EntreAmigos")
        appendLine(lineaBalance)
        if (detalleDeudas.isNotBlank()) {
            appendLine()
            appendLine("Deudas del grupo:")
            append(detalleDeudas)
        }
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Resumen de $nombreGrupo")
        putExtra(Intent.EXTRA_TEXT, texto)
    }
    context.startActivity(Intent.createChooser(intent, "Compartir resumen vía"))
}
