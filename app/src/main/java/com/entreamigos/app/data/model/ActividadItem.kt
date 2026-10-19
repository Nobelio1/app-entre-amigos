package com.entreamigos.app.data.model

import com.entreamigos.app.data.local.entity.GrupoIcono

data class ActividadItem(
    val gasto: Gasto,
    val nombreGrupo: String,
    val iconoGrupo: GrupoIcono
)
