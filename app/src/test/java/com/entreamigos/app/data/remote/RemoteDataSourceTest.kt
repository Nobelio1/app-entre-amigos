package com.entreamigos.app.data.remote

import com.entreamigos.app.data.model.Resource
import com.entreamigos.app.data.model.TipoError
import com.entreamigos.app.data.remote.dto.CrearGastoRequest
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Prueba la integración Retrofit + kotlinx.serialization + traducción de errores contra
 * un servidor HTTP simulado: lo que viaja por el cable es real, solo el backend es falso.
 */
class RemoteDataSourceTest {

    private lateinit var servidor: MockWebServer
    private lateinit var remoto: RemoteDataSource

    @Before
    fun setUp() {
        servidor = MockWebServer().apply { start() }
        val cliente = OkHttpClient.Builder()
            .readTimeout(1, TimeUnit.SECONDS)
            .build()
        val api = NetworkModule.crearRetrofit(servidor.url("/").toString(), cliente)
            .create(EntreAmigosApi::class.java)
        remoto = RemoteDataSource(api)
    }

    @After
    fun tearDown() {
        servidor.shutdown()
    }

    private fun json(codigo: Int, cuerpo: String) =
        MockResponse().setResponseCode(codigo).setHeader("Content-Type", "application/json").setBody(cuerpo)

    @Test
    fun `respuesta 200 se deserializa en Success`() = runTest {
        servidor.enqueue(
            json(
                200,
                """
                [{"id":1,"nombre":"Viaje a Punta Cana","icono":"VIAJE","colorHex":"#1B5E4F",
                  "fechaCreacion":1716508800000,"campoNuevoDelServidor":true,
                  "miembros":[{"id":10,"grupoId":1,"nombre":"Ana","esUsuarioActual":true,"colorAvatarHex":"#EF5350"}]}]
                """.trimIndent()
            )
        )

        val resultado = remoto.llamar { listarGrupos() }

        assertTrue("Se esperaba Success pero fue $resultado", resultado is Resource.Success)
        val grupo = (resultado as Resource.Success).data.single()
        assertEquals("Viaje a Punta Cana", grupo.nombre)
        assertEquals("Ana", grupo.miembros.single().nombre)
        assertEquals("/grupos", servidor.takeRequest().path)
    }

    @Test
    fun `POST envia el cuerpo JSON esperado por el backend`() = runTest {
        servidor.enqueue(
            json(
                201,
                """{"id":7,"grupoId":3,"descripcion":"Cena","monto":100.0,"pagadoPorId":10,"categoria":"COMIDA",
                   "fecha":1,"participantes":[{"miembroId":10,"montoAsignado":50.0},{"miembroId":11,"montoAsignado":50.0}]}"""
            )
        )

        val resultado = remoto.llamar {
            registrarGasto(3, CrearGastoRequest("Cena", 100.0, 10, "COMIDA", listOf(10, 11)))
        }

        assertEquals(7L, (resultado as Resource.Success).data.id)
        val peticion = servidor.takeRequest()
        assertEquals("POST", peticion.method)
        assertEquals("/grupos/3/gastos", peticion.path)
        val cuerpo = Json.parseToJsonElement(peticion.body.readUtf8()).jsonObject
        assertEquals("Cena", cuerpo["descripcion"]!!.jsonPrimitive.content)
        assertEquals(2, cuerpo["participantes"]!!.jsonArray.size)
        // explicitNulls = false: la fecha nula no se envía
        assertTrue("fecha" !in cuerpo)
    }

    @Test
    fun `error 404 usa el mensaje del formato estandar del backend`() = runTest {
        servidor.enqueue(json(404, """{"error":"Grupo 99 no encontrado"}"""))

        val resultado = remoto.llamar { obtenerGrupo(99) }

        assertEquals(Resource.Error("Grupo 99 no encontrado", TipoError.SOLICITUD_INVALIDA), resultado)
    }

    @Test
    fun `error 400 de validacion se clasifica como solicitud invalida`() = runTest {
        servidor.enqueue(json(400, """{"error":"El monto debe ser mayor a cero"}"""))

        val resultado = remoto.llamar { registrarGasto(1, CrearGastoRequest("X", -1.0, 1, "OTROS", listOf(1))) }

        assertEquals(Resource.Error("El monto debe ser mayor a cero", TipoError.SOLICITUD_INVALIDA), resultado)
    }

    @Test
    fun `error 500 sin cuerpo JSON usa un mensaje generico de servidor`() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(500).setBody("<html>Internal Error</html>"))

        val resultado = remoto.llamar { listarGrupos() }

        assertEquals(Resource.Error("El servidor respondió con el código 500", TipoError.SERVIDOR), resultado)
    }

    @Test
    fun `JSON con forma inesperada no crashea la app`() = runTest {
        servidor.enqueue(json(200, """{"esto":"no es una lista"}"""))

        val resultado = remoto.llamar { listarGrupos() }

        assertEquals(TipoError.SERVIDOR, (resultado as Resource.Error).tipo)
    }

    @Test
    fun `servidor que no responde se reporta como sin conexion`() = runTest {
        servidor.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))

        val resultado = remoto.llamar { listarGrupos() }

        assertEquals(TipoError.SIN_CONEXION, (resultado as Resource.Error).tipo)
    }

    @Test
    fun `servidor apagado se reporta como sin conexion`() = runTest {
        servidor.shutdown()

        val resultado = remoto.llamar { listarGrupos() }

        assertEquals(TipoError.SIN_CONEXION, (resultado as Resource.Error).tipo)
    }
}
