package com.entreamigos.app.data.remote

import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * Construcción manual de Retrofit/OkHttp (el proyecto no usa Hilt; ver AppContainer).
 * La base URL llega desde BuildConfig.API_BASE_URL y nunca está escrita aquí.
 */
object NetworkModule {

    /**
     * `ignoreUnknownKeys` permite que el backend agregue campos sin romper versiones
     * viejas de la app; `explicitNulls = false` evita enviar `"fecha": null`.
     */
    val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    fun crearApi(baseUrl: String, registrarTrafico: Boolean): EntreAmigosApi =
        crearRetrofit(baseUrl, crearOkHttp(registrarTrafico)).create(EntreAmigosApi::class.java)

    fun crearOkHttp(registrarTrafico: Boolean): OkHttpClient =
        OkHttpClient.Builder()
            // Timeouts cortos: si el backend no responde pronto la app pasa a modo
            // offline y sigue funcionando con Room, en vez de dejar al usuario esperando.
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .apply {
                if (registrarTrafico) {
                    addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                }
            }
            .build()

    fun crearRetrofit(baseUrl: String, cliente: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(cliente)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
}
