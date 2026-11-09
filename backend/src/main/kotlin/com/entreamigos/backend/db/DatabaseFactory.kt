package com.entreamigos.backend.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory
import java.sql.SQLException
import javax.sql.DataSource

data class ConfigBaseDatos(
    val url: String,
    val usuario: String,
    val password: String,
    val maxPoolSize: Int,
    val cargarSeed: Boolean = false
)

object DatabaseFactory {

    private val log = LoggerFactory.getLogger(DatabaseFactory::class.java)

    private const val RUTA_SEED = "db/seed.sql"

    fun inicializar(config: ConfigBaseDatos) {
        val dataSource = HikariDataSource(HikariConfig().apply {
            jdbcUrl = config.url
            username = config.usuario
            password = config.password
            maximumPoolSize = config.maxPoolSize
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_READ_COMMITTED"
            validate()
        })

        val resultado = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .load()
            .migrate()
        log.info("Migraciones aplicadas: {} (esquema en v{})", resultado.migrationsExecuted, resultado.targetSchemaVersion)

        if (config.cargarSeed) cargarSeedSiBaseVacia(dataSource)

        Database.connect(dataSource)
    }

    /**
     * Ejecuta `db/seed.sql` solo en una base recién creada (sin usuarios), para que
     * reiniciar la API no "resucite" datos de demo que alguien ya borró o editó.
     */
    private fun cargarSeedSiBaseVacia(dataSource: DataSource) {
        val sql = DatabaseFactory::class.java.classLoader.getResource(RUTA_SEED)?.readText()
        if (sql == null) {
            log.warn("No se encontró {} en el classpath; se omite la carga de datos semilla", RUTA_SEED)
            return
        }
        dataSource.connection.use { conexion ->
            val hayUsuarios = conexion.createStatement().use { st ->
                st.executeQuery("SELECT EXISTS (SELECT 1 FROM usuarios)").use { rs -> rs.next() && rs.getBoolean(1) }
            }
            if (hayUsuarios) {
                conexion.rollback()
                log.info("La base ya tiene usuarios; no se carga {}", RUTA_SEED)
                return
            }
            try {
                conexion.createStatement().use { it.execute(sql) }
                conexion.commit()
                log.info("Datos semilla cargados desde {}", RUTA_SEED)
            } catch (e: Exception) {
                conexion.rollback()
                throw IllegalStateException("No se pudo cargar $RUTA_SEED", e)
            }
        }
    }

    suspend fun <T> consulta(bloque: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { bloque() }
}

/** `23505` = unique_violation en PostgreSQL. */
fun Throwable.esViolacionDeUnicidad(): Boolean =
    generateSequence(this) { it.cause }.any { (it as? SQLException)?.sqlState == "23505" }
