package com.entreamigos.backend.db

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object Usuarios : Table("usuarios") {
    val id = long("id").autoIncrement()
    val nombre = varchar("nombre", 80)
    val email = varchar("email", 160)
    /** null en cuentas creadas con Google. */
    val passwordHash = varchar("password_hash", 100).nullable()
    val colorAvatarHex = varchar("color_avatar_hex", 9)
    val fechaCreacion = long("fecha_creacion")
    val firebaseUid = varchar("firebase_uid", 128).nullable()
    val fotoUrl = varchar("foto_url", 500).nullable()
    override val primaryKey = PrimaryKey(id)
}

object Grupos : Table("grupos") {
    val id = long("id").autoIncrement()
    val nombre = varchar("nombre", 120)
    val icono = varchar("icono", 40)
    val colorHex = varchar("color_hex", 9)
    val fechaCreacion = long("fecha_creacion")
    val creadoPor = long("creado_por").references(Usuarios.id, onDelete = ReferenceOption.SET_NULL).nullable()
    val codigoInvitacion = varchar("codigo_invitacion", 12)
    val idempotencyKey = varchar("idempotency_key", 64).nullable()
    override val primaryKey = PrimaryKey(id)
}

object Miembros : Table("miembros") {
    val id = long("id").autoIncrement()
    val grupoId = long("grupo_id").references(Grupos.id, onDelete = ReferenceOption.CASCADE)
    val nombre = varchar("nombre", 80)
    val colorAvatarHex = varchar("color_avatar_hex", 9)
    val usuarioId = long("usuario_id").references(Usuarios.id, onDelete = ReferenceOption.SET_NULL).nullable()
    val idempotencyKey = varchar("idempotency_key", 64).nullable()
    override val primaryKey = PrimaryKey(id)
}

object Gastos : Table("gastos") {
    val id = long("id").autoIncrement()
    val grupoId = long("grupo_id").references(Grupos.id, onDelete = ReferenceOption.CASCADE)
    val descripcion = varchar("descripcion", 200)
    val monto = decimal("monto", 12, 2)
    val pagadoPorId = long("pagado_por_id").references(Miembros.id, onDelete = ReferenceOption.CASCADE)
    val categoria = varchar("categoria", 30)
    val fecha = long("fecha")
    val fechaActualizacion = long("fecha_actualizacion").nullable()
    val idempotencyKey = varchar("idempotency_key", 64).nullable()
    override val primaryKey = PrimaryKey(id)
}

object GastoParticipantes : Table("gasto_participantes") {
    val id = long("id").autoIncrement()
    val gastoId = long("gasto_id").references(Gastos.id, onDelete = ReferenceOption.CASCADE)
    val miembroId = long("miembro_id").references(Miembros.id, onDelete = ReferenceOption.CASCADE)
    val montoAsignado = decimal("monto_asignado", 12, 2)
    override val primaryKey = PrimaryKey(id)
}

object Pagos : Table("pagos") {
    val id = long("id").autoIncrement()
    val grupoId = long("grupo_id").references(Grupos.id, onDelete = ReferenceOption.CASCADE)
    val deudorId = long("deudor_id").references(Miembros.id, onDelete = ReferenceOption.CASCADE)
    val acreedorId = long("acreedor_id").references(Miembros.id, onDelete = ReferenceOption.CASCADE)
    val monto = decimal("monto", 12, 2)
    val fecha = long("fecha")
    val idempotencyKey = varchar("idempotency_key", 64).nullable()
    override val primaryKey = PrimaryKey(id)
}
