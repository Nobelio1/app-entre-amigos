package com.entreamigos.app.data.remote

import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object NetworkModule {

    val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    fun crearApi(
        baseUrl: String,
        registrarTrafico: Boolean,
        token: () -> String? = { null },
        onNoAutorizado: () -> Unit = {}
    ): EntreAmigosApi =
        crearRetrofit(baseUrl, crearOkHttp(registrarTrafico, token, onNoAutorizado)).create(EntreAmigosApi::class.java)

    fun crearOkHttp(
        registrarTrafico: Boolean,
        token: () -> String? = { null },
        onNoAutorizado: () -> Unit = {}
    ): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(interceptorAutenticacion(token, onNoAutorizado))
            .apply {
                if (registrarTrafico) {
                    addInterceptor(
                        HttpLoggingInterceptor()
                            .setLevel(HttpLoggingInterceptor.Level.BODY)
                            .apply { redactHeader("Authorization") }
                    )
                }
            }
            .build()

    /**
     * Agrega `Authorization: Bearer <token>` cuando hay sesión. Si el servidor responde 401
     * a una petición que sí llevaba token, la sesión expiró o fue revocada: se avisa con
     * [onNoAutorizado] para volver a la pantalla de inicio de sesión.
     */
    fun interceptorAutenticacion(token: () -> String?, onNoAutorizado: () -> Unit) = Interceptor { chain ->
        val actual = token()
        val peticion = if (actual != null) {
            chain.request().newBuilder().header("Authorization", "Bearer $actual").build()
        } else {
            chain.request()
        }
        val respuesta = chain.proceed(peticion)
        if (respuesta.code == 401 && actual != null) onNoAutorizado()
        respuesta
    }

    fun crearRetrofit(baseUrl: String, cliente: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(cliente)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
}
