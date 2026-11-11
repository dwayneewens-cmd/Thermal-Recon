package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ThermalReportEntity::class], version = 1, exportSchema = false)
abstract class ThermalDatabase : RoomDatabase() {
    abstract fun thermalReportDao(): ThermalReportDao

    companion object {
        @Volatile
        private var INSTANCE: ThermalDatabase? = null

        fun getInstance(context: Context): ThermalDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ThermalDatabase::class.java,
                    "thermal_recon_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
