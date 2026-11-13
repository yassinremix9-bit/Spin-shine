package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.NotificationEntity
import com.example.data.model.WashingMachineEntity

@Database(
    entities = [WashingMachineEntity::class, NotificationEntity::class],
    version = 2,
    exportSchema = false
)
abstract class SpinShineDatabase : RoomDatabase() {
    abstract fun machineDao(): MachineDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile
        private var INSTANCE: SpinShineDatabase? = null

        fun getInstance(context: Context): SpinShineDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SpinShineDatabase::class.java,
                    "spin_shine_db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
