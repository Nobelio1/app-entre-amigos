package com.entreamigos.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.entreamigos.app.data.local.dao.GastoDao
import com.entreamigos.app.data.local.dao.GrupoDao
import com.entreamigos.app.data.local.dao.MiembroDao
import com.entreamigos.app.data.local.dao.PagoDao
import com.entreamigos.app.data.local.entity.CategoriaGasto
import com.entreamigos.app.data.local.entity.GastoEntity
import com.entreamigos.app.data.local.entity.GastoParticipanteEntity
import com.entreamigos.app.data.local.entity.GrupoEntity
import com.entreamigos.app.data.local.entity.GrupoIcono
import com.entreamigos.app.data.local.entity.MiembroEntity
import com.entreamigos.app.data.local.entity.PagoEntity

class Converters {
    @TypeConverter
    fun fromCategoria(categoria: CategoriaGasto): String = categoria.name

    @TypeConverter
    fun toCategoria(valor: String): CategoriaGasto = CategoriaGasto.valueOf(valor)

    @TypeConverter
    fun fromIcono(icono: GrupoIcono): String = icono.name

    @TypeConverter
    fun toIcono(valor: String): GrupoIcono = GrupoIcono.desdeNombre(valor)
}

@Database(
    entities = [
        GrupoEntity::class,
        MiembroEntity::class,
        GastoEntity::class,
        GastoParticipanteEntity::class,
        PagoEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun grupoDao(): GrupoDao
    abstract fun miembroDao(): MiembroDao
    abstract fun gastoDao(): GastoDao
    abstract fun pagoDao(): PagoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instancia = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "entreamigos_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instancia
                instancia
            }
        }
    }
}
