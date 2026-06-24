package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [DeliveryStop::class, LocationLog::class, SyncQueue::class, UserAccount::class, CollaborativePin::class],
    version = 2,
    exportSchema = false
)
abstract class DeliveryDatabase : RoomDatabase() {
    abstract fun deliveryDao(): DeliveryDao

    companion object {
        @Volatile
        private var INSTANCE: DeliveryDatabase? = null

        fun getDatabase(context: Context): DeliveryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DeliveryDatabase::class.java,
                    "omniroute_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
