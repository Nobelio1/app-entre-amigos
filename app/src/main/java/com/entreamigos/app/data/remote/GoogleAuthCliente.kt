package com.entreamigos.app.data.remote

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

sealed interface ResultadoGoogle {
    /** ID token de Firebase listo para enviarse al backend (`POST /auth/google`). */
    data class Token(val idTokenFirebase: String) : ResultadoGoogle
    data object Cancelado : ResultadoGoogle
    data class Error(val mensaje: String, val sinConexion: Boolean = false) : ResultadoGoogle
}

/**
 * "Continuar con Google":
 *  1. Credential Manager muestra el selector de cuentas de Google y devuelve un ID token de Google.
 *  2. Con ese token se inicia sesión en Firebase Authentication.
 *  3. Firebase entrega su propio ID token, que el backend verifica para emitir el JWT de la app.
 *
 * Depende de `app/google-services.json`; si no está, [disponible] es false y el botón no se muestra.
 */
class GoogleAuthCliente(private val context: Context) {

    /** OAuth "Web client ID" que el plugin google-services genera como recurso string. */
    @SuppressLint("DiscouragedApi") // el recurso solo existe si google-services.json está presente
    private val webClientId: String? =
        context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            .takeIf { it != 0 }
            ?.let(context::getString)
            ?.takeIf { it.isNotBlank() }

    val disponible: Boolean
        get() = webClientId != null && FirebaseApp.getApps(context).isNotEmpty()

    /** [contextoActividad] debe ser la Activity: Credential Manager muestra UI sobre ella. */
    suspend fun obtenerToken(contextoActividad: Context): ResultadoGoogle {
        val clientId = webClientId
        if (clientId == null || !disponible) {
            return ResultadoGoogle.Error("El inicio de sesión con Google no está configurado en esta compilación")
        }

        val credencial = try {
            val solicitud = GetCredentialRequest.Builder()
                .addCredentialOption(GetSignInWithGoogleOption.Builder(clientId).build())
                .build()
            CredentialManager.create(contextoActividad).getCredential(contextoActividad, solicitud).credential
        } catch (e: CancellationException) {
            throw e
        } catch (e: GetCredentialCancellationException) {
            return ResultadoGoogle.Cancelado
        } catch (e: NoCredentialException) {
            return ResultadoGoogle.Error("No hay cuentas de Google disponibles en este dispositivo")
        } catch (e: GetCredentialException) {
            Log.w(TAG, "Credential Manager falló", e)
            return ResultadoGoogle.Error("No se pudo iniciar sesión con Google (${e.type})")
        }

        if (credencial !is CustomCredential ||
            credencial.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            return ResultadoGoogle.Error("Google devolvió una credencial inesperada")
        }

        return try {
            val idTokenGoogle = GoogleIdTokenCredential.createFrom(credencial.data).idToken
            val auth = FirebaseAuth.getInstance()
            val usuario = auth.signInWithCredential(GoogleAuthProvider.getCredential(idTokenGoogle, null)).await().user
                ?: return ResultadoGoogle.Error("Firebase no devolvió un usuario")
            val token = usuario.getIdToken(true).await().token
                ?: return ResultadoGoogle.Error("Firebase no devolvió un token")
            ResultadoGoogle.Token(token)
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseNetworkException) {
            ResultadoGoogle.Error("Sin conexión: no se pudo contactar con Google", sinConexion = true)
        } catch (e: Exception) {
            Log.w(TAG, "Firebase Authentication falló", e)
            ResultadoGoogle.Error("Firebase rechazó el inicio de sesión: ${e.localizedMessage ?: e.javaClass.simpleName}")
        }
    }

    /** Cierra la sesión de Firebase y olvida la cuenta elegida, para poder escoger otra la próxima vez. */
    suspend fun cerrarSesion() {
        if (!disponible) return
        FirebaseAuth.getInstance().signOut()
        try {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo limpiar el estado de Credential Manager", e)
        }
    }

    private companion object {
        const val TAG = "GoogleAuth"
    }
}
