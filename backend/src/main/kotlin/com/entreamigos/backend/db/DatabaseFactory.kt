package com.entreamigos.backend.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory

data class ConfigBaseDatos(val url: String, val usuario: String, val password: String, val maxPoolSize: Int)

object DatabaseFactory {

    private val log = LoggerFactory.getLogger(DatabaseFactory::class.java)

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

        Database.connect(dataSource)
    }

    suspend fun <T> consulta(bloque: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { bloque() }
}
