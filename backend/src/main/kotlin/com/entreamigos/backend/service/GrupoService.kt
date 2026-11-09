package com.entreamigos.backend.service

import com.entreamigos.backend.db.DatabaseFactory.consulta
import com.entreamigos.backend.db.Grupos
import com.entreamigos.backend.db.Miembros
import com.entreamigos.backend.db.Usuarios
import com.entreamigos.backend.model.CrearGrupoRequest
import com.entreamigos.backend.model.CrearMiembroRequest
import com.entreamigos.backend.model.GrupoDto
import com.entreamigos.backend.model.InvitacionDto
import com.entreamigos.backend.model.MiembroDto
import com.entreamigos.backend.model.MiembroInvitacionDto
import com.entreamigos.backend.model.UnirseGrupoRequest
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

class GrupoService {

    /** Grupos en los que el usuario es miembro. */
    suspend fun listar(usuarioId: Long): List<GrupoDto> = consulta {
        val grupoIds = Miembros.select(Miembros.grupoId)
            .where { Miembros.usuarioId eq usuarioId }
            .map { it[Miembros.grupoId] }
        if (grupoIds.isEmpty()) return@consulta emptyList()

        val miembrosPorGrupo = Miembros.selectAll()
            .where { Miembros.grupoId inList grupoIds }
            .orderBy(Miembros.id)
            .map { it.aMiembroDto(usuarioId) }
            .groupBy { it.grupoId }
        Grupos.selectAll()
            .where { Grupos.id inList grupoIds }
            .orderBy(Grupos.fechaCreacion, SortOrder.DESC)
            .map { it.aGrupoDto(miembrosPorGrupo[it[Grupos.id]].orEmpty()) }
    }

    suspend fun obtener(grupoId: Long, usuarioId: Long): GrupoDto = consulta {
        miembrosConAcceso(grupoId, usuarioId)
        obtenerEnTransaccion(grupoId, usuarioId)
    }

    suspend fun crear(request: CrearGrupoRequest, usuarioId: Long, clave: String?): Creado<GrupoDto> {
        val nombre = request.nombre.trim()
        validar(nombre.isNotEmpty()) { "El nombre del grupo es obligatorio" }
        validar(nombre.length <= 120) { "El nombre del grupo no puede superar 120 caracteres" }
        validar(COLOR_HEX.matches(request.colorHex)) { "colorHex debe tener el formato #RRGGBB" }
        validar(request.icono.isNotBlank()) { "El icono es obligatorio" }
        val miembros = request.miembros.map { it.normalizado() }
        validar(miembros.size >= 2) { "Un grupo necesita al menos 2 miembros" }
        validar(miembros.count { it.esUsuarioActual } <= 1) { "Solo un miembro puede ser el usuario actual" }
        // El creador queda vinculado al miembro marcado como usuario actual (o al primero)
        val indiceCreador = miembros.indexOfFirst { it.esUsuarioActual }.takeIf { it >= 0 } ?: 0

        return crearIdempotente(
            clave = clave,
            buscar = { k ->
                val fila = Grupos.selectAll().where { Grupos.idempotencyKey eq k }.singleOrNull()
                    ?: return@crearIdempotente null
                if (fila[Grupos.creadoPor] != usuarioId) throw ConflictoException(CLAVE_REUTILIZADA)
                obtenerEnTransaccion(fila[Grupos.id], usuarioId)
            },
            crear = {
                val grupoId = Grupos.insert {
                    it[Grupos.nombre] = nombre
                    it[icono] = request.icono.trim().uppercase()
                    it[colorHex] = request.colorHex
                    it[fechaCreacion] = request.fechaCreacion ?: System.currentTimeMillis()
                    it[creadoPor] = usuarioId
                    it[codigoInvitacion] = generarCodigoUnico()
                    it[idempotencyKey] = clave
                } get Grupos.id

                miembros.forEachIndexed { indice, miembro ->
                    insertarMiembro(
                        grupoId = grupoId,
                        nombre = miembro.nombre,
                        color = miembro.colorAvatarHex ?: PALETA_AVATARES[indice % PALETA_AVATARES.size],
                        usuarioId = usuarioId.takeIf { indice == indiceCreador }
                    )
                }
                obtenerEnTransaccion(grupoId, usuarioId)
            }
        )
    }

    suspend fun agregarMiembro(
        grupoId: Long,
        request: CrearMiembroRequest,
        usuarioId: Long,
        clave: String?
    ): Creado<MiembroDto> {
        val miembro = request.normalizado()
        consulta { miembrosConAcceso(grupoId, usuarioId) }
        return crearIdempotente(
            clave = clave,
            buscar = { k ->
                val fila = Miembros.selectAll().where { Miembros.idempotencyKey eq k }.singleOrNull()
                    ?: return@crearIdempotente null
                if (fila[Miembros.grupoId] != grupoId) throw ConflictoException(CLAVE_REUTILIZADA)
                fila.aMiembroDto(usuarioId)
            },
            crear = {
                val existentes = miembrosDe(grupoId, usuarioId).orEmpty()
                validar(existentes.none { it.nombre.equals(miembro.nombre, ignoreCase = true) }) {
                    "Ya existe un miembro llamado '${miembro.nombre}' en el grupo"
                }
                val id = insertarMiembro(
                    grupoId = grupoId,
                    nombre = miembro.nombre,
                    color = miembro.colorAvatarHex ?: PALETA_AVATARES[existentes.size % PALETA_AVATARES.size],
                    usuarioId = null,
                    clave = clave
                )
                Miembros.selectAll().where { Miembros.id eq id }.single().aMiembroDto(usuarioId)
            }
        )
    }

    /** Vista previa de un grupo a partir de su código, para elegir con qué miembro unirse. */
    suspend fun verInvitacion(codigo: String): InvitacionDto = consulta {
        val grupo = grupoPorCodigo(codigo)
        val grupoId = grupo[Grupos.id]
        InvitacionDto(
            grupoId = grupoId,
            nombre = grupo[Grupos.nombre],
            icono = grupo[Grupos.icono],
            colorHex = grupo[Grupos.colorHex],
            codigo = grupo[Grupos.codigoInvitacion],
            miembros = Miembros.selectAll().where { Miembros.grupoId eq grupoId }
                .orderBy(Miembros.id)
                .map {
                    MiembroInvitacionDto(
                        id = it[Miembros.id],
                        nombre = it[Miembros.nombre],
                        colorAvatarHex = it[Miembros.colorAvatarHex],
                        vinculado = it[Miembros.usuarioId] != null
                    )
                }
        )
    }

    /**
     * Une al usuario a un grupo. Si indica [UnirseGrupoRequest.miembroId] se vincula a ese
     * integrante (debe estar libre); si no, se reutiliza un integrante libre con su mismo
     * nombre o se crea uno nuevo. Repetir la operación es inofensivo.
     */
    suspend fun unirse(request: UnirseGrupoRequest, usuarioId: Long): Creado<GrupoDto> = consulta {
        val grupoId = grupoPorCodigo(request.codigo)[Grupos.id]
        val miembros = Miembros.selectAll().where { Miembros.grupoId eq grupoId }.toList()

        if (miembros.any { it[Miembros.usuarioId] == usuarioId }) {
            return@consulta Creado(obtenerEnTransaccion(grupoId, usuarioId), nuevo = false)
        }

        val usuario = Usuarios.selectAll().where { Usuarios.id eq usuarioId }.singleOrNull()
            ?: throw NoAutenticadoException("La cuenta ya no existe")
        val libres = miembros.filter { it[Miembros.usuarioId] == null }

        val elegido = if (request.miembroId != null) {
            val fila = miembros.firstOrNull { it[Miembros.id] == request.miembroId }
                ?: throw SolicitudInvalidaException("miembroId no pertenece al grupo")
            if (fila[Miembros.usuarioId] != null) throw ConflictoException("Ese integrante ya está vinculado a otra cuenta")
            fila
        } else {
            libres.firstOrNull { it[Miembros.nombre].equals(usuario[Usuarios.nombre], ignoreCase = true) }
        }

        if (elegido != null) {
            Miembros.update({ (Miembros.id eq elegido[Miembros.id]) and Miembros.usuarioId.isNull() }) {
                it[Miembros.usuarioId] = usuarioId
            }
        } else {
            val nombreBase = usuario[Usuarios.nombre]
            val usados = miembros.map { it[Miembros.nombre].lowercase() }.toSet()
            val nombre = generateSequence(1) { it + 1 }
                .map { n -> if (n == 1) nombreBase else "$nombreBase $n" }
                .first { it.lowercase() !in usados }
            insertarMiembro(grupoId, nombre, usuario[Usuarios.colorAvatarHex], usuarioId)
        }
        Creado(obtenerEnTransaccion(grupoId, usuarioId), nuevo = true)
    }

    /**
     * Debe llamarse dentro de una transacción. Devuelve los miembros del grupo si el
     * usuario pertenece a él; 404 si el grupo no existe y 403 si no es miembro.
     */
    fun miembrosConAcceso(grupoId: Long, usuarioId: Long): List<MiembroDto> {
        val miembros = miembrosDe(grupoId, usuarioId)
            ?: throw RecursoNoEncontradoException("Grupo $grupoId no encontrado")
        if (miembros.none { it.usuarioId == usuarioId }) throw SinPermisoException("No perteneces a este grupo")
        return miembros
    }

    private fun miembrosDe(grupoId: Long, usuarioActualId: Long?): List<MiembroDto>? {
        val existe = Grupos.selectAll().where { Grupos.id eq grupoId }.any()
        if (!existe) return null
        return Miembros.selectAll().where { Miembros.grupoId eq grupoId }
            .orderBy(Miembros.id)
            .map { it.aMiembroDto(usuarioActualId) }
    }

    private fun obtenerEnTransaccion(grupoId: Long, usuarioId: Long): GrupoDto {
        val fila = Grupos.selectAll().where { Grupos.id eq grupoId }.singleOrNull()
            ?: throw RecursoNoEncontradoException("Grupo $grupoId no encontrado")
        return fila.aGrupoDto(miembrosDe(grupoId, usuarioId).orEmpty())
    }

    private fun grupoPorCodigo(codigo: String): ResultRow {
        val normalizado = codigo.trim().uppercase()
        validar(normalizado.isNotEmpty()) { "Ingresa el código de invitación" }
        return Grupos.selectAll().where { Grupos.codigoInvitacion eq normalizado }.singleOrNull()
            ?: throw RecursoNoEncontradoException("No existe un grupo con el código $normalizado")
    }

    private fun generarCodigoUnico(): String =
        generateSequence { (1..LONGITUD_CODIGO).map { ALFABETO_CODIGO.random() }.joinToString("") }
            .first { candidato -> Grupos.selectAll().where { Grupos.codigoInvitacion eq candidato }.empty() }

    private fun insertarMiembro(grupoId: Long, nombre: String, color: String, usuarioId: Long?, clave: String? = null): Long =
        Miembros.insert {
            it[Miembros.grupoId] = grupoId
            it[Miembros.nombre] = nombre
            it[colorAvatarHex] = color
            it[Miembros.usuarioId] = usuarioId
            it[idempotencyKey] = clave
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
        codigoInvitacion = this[Grupos.codigoInvitacion],
        miembros = miembros
    )

    companion object {
        private val COLOR_HEX = Regex("^#[0-9A-Fa-f]{6}$")
        private const val LONGITUD_CODIGO = 6
        // Sin caracteres ambiguos (0/O, 1/I/L)
        private const val ALFABETO_CODIGO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
        const val CLAVE_REUTILIZADA = "La Idempotency-Key ya se usó para otro recurso"

        val PALETA_AVATARES = listOf("#EF5350", "#42A5F5", "#66BB6A", "#AB47BC", "#FFA726", "#26C6DA")
    }
}

fun ResultRow.aMiembroDto(usuarioActualId: Long?) = MiembroDto(
    id = this[Miembros.id],
    grupoId = this[Miembros.grupoId],
    nombre = this[Miembros.nombre],
    esUsuarioActual = usuarioActualId != null && this[Miembros.usuarioId] == usuarioActualId,
    colorAvatarHex = this[Miembros.colorAvatarHex],
    usuarioId = this[Miembros.usuarioId]
)
