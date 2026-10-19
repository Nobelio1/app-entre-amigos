package com.entreamigos.backend.service

import com.entreamigos.backend.db.DatabaseFactory.consulta
import com.entreamigos.backend.db.Grupos
import com.entreamigos.backend.db.Miembros
import com.entreamigos.backend.model.CrearGrupoRequest
import com.entreamigos.backend.model.CrearMiembroRequest
import com.entreamigos.backend.model.GrupoDto
import com.entreamigos.backend.model.MiembroDto
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll

class GrupoService {

    suspend fun listar(): List<GrupoDto> = consulta {
        val miembrosPorGrupo = Miembros.selectAll()
            .orderBy(Miembros.id)
            .map { it.aMiembroDto() }
            .groupBy { it.grupoId }
        Grupos.selectAll()
            .orderBy(Grupos.fechaCreacion, SortOrder.DESC)
            .map { it.aGrupoDto(miembrosPorGrupo[it[Grupos.id]].orEmpty()) }
    }

    suspend fun obtener(grupoId: Long): GrupoDto = consulta { obtenerEnTransaccion(grupoId) }

    suspend fun crear(request: CrearGrupoRequest): GrupoDto {
        val nombre = request.nombre.trim()
        validar(nombre.isNotEmpty()) { "El nombre del grupo es obligatorio" }
        validar(nombre.length <= 120) { "El nombre del grupo no puede superar 120 caracteres" }
        validar(COLOR_HEX.matches(request.colorHex)) { "colorHex debe tener el formato #RRGGBB" }
        validar(request.icono.isNotBlank()) { "El icono es obligatorio" }
        val miembros = request.miembros.map { it.normalizado() }
        validar(miembros.size >= 2) { "Un grupo necesita al menos 2 miembros" }
        validar(miembros.count { it.esUsuarioActual } <= 1) { "Solo un miembro puede ser el usuario actual" }

        return consulta {
            val grupoId = Grupos.insert {
                it[Grupos.nombre] = nombre
                it[icono] = request.icono.trim().uppercase()
                it[colorHex] = request.colorHex
                it[fechaCreacion] = request.fechaCreacion ?: System.currentTimeMillis()
            } get Grupos.id

            miembros.forEachIndexed { indice, miembro -> insertarMiembro(grupoId, miembro, indice) }
            obtenerEnTransaccion(grupoId)
        }
    }

    suspend fun agregarMiembro(grupoId: Long, request: CrearMiembroRequest): MiembroDto {
        val miembro = request.normalizado()
        return consulta {
            val existentes = miembrosDe(grupoId)
            if (existentes == null) throw RecursoNoEncontradoException("Grupo $grupoId no encontrado")
            validar(existentes.none { it.nombre.equals(miembro.nombre, ignoreCase = true) }) {
                "Ya existe un miembro llamado '${miembro.nombre}' en el grupo"
            }
            validar(!miembro.esUsuarioActual || existentes.none { it.esUsuarioActual }) {
                "El grupo ya tiene un usuario actual"
            }
            val id = insertarMiembro(grupoId, miembro, existentes.size)
            Miembros.selectAll().where { Miembros.id eq id }.single().aMiembroDto()
        }
    }

    /** Debe llamarse dentro de una transacción. Devuelve null si el grupo no existe. */
    fun miembrosDe(grupoId: Long): List<MiembroDto>? {
        val existe = Grupos.selectAll().where { Grupos.id eq grupoId }.any()
        if (!existe) return null
        return Miembros.selectAll().where { Miembros.grupoId eq grupoId }
            .orderBy(Miembros.id)
            .map { it.aMiembroDto() }
    }

    private fun obtenerEnTransaccion(grupoId: Long): GrupoDto {
        val fila = Grupos.selectAll().where { Grupos.id eq grupoId }.singleOrNull()
            ?: throw RecursoNoEncontradoException("Grupo $grupoId no encontrado")
        return fila.aGrupoDto(miembrosDe(grupoId).orEmpty())
    }

    private fun insertarMiembro(grupoId: Long, miembro: CrearMiembroRequest, indice: Int): Long =
        Miembros.insert {
            it[Miembros.grupoId] = grupoId
            it[nombre] = miembro.nombre
            it[esUsuarioActual] = miembro.esUsuarioActual
            it[colorAvatarHex] = miembro.colorAvatarHex ?: PALETA_AVATARES[indice % PALETA_AVATARES.size]
        } get Miembros.id

    private fun CrearMiembroRequest.normalizado(): CrearMiembroRequest {
        val limpio = nombre.trim()
        validar(limpio.isNotEmpty()) { "El nombre del miembro es obligatorio" }
        validar(limpio.length <= 80) { "El nombre del miembro no puede superar 80 caracteres" }
        colorAvatarHex?.let { validar(COLOR_HEX.matches(it)) { "colorAvatarHex debe tener el formato #RRGGBB" } }
        return copy(nombre = limpio)
    }

    private fun ResultRow.aGrupoDto(miembros: List<MiembroDto>) = GrupoDto(
        id = this[Grupos.id],
        nombre = this[Grupos.nombre],
        icono = this[Grupos.icono],
        colorHex = this[Grupos.colorHex],
        fechaCreacion = this[Grupos.fechaCreacion],
        miembros = miembros
    )

    companion object {
        private val COLOR_HEX = Regex("^#[0-9A-Fa-f]{6}$")

        /** Misma paleta que `GrupoRepository.PALETA_AVATARES` en la app. */
        val PALETA_AVATARES = listOf("#EF5350", "#42A5F5", "#66BB6A", "#AB47BC", "#FFA726", "#26C6DA")
    }
}

fun ResultRow.aMiembroDto() = MiembroDto(
    id = this[Miembros.id],
    grupoId = this[Miembros.grupoId],
    nombre = this[Miembros.nombre],
    esUsuarioActual = this[Miembros.esUsuarioActual],
    colorAvatarHex = this[Miembros.colorAvatarHex]
)
