package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PresetEntity::class, AudioProjectEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SoundLabDatabase : RoomDatabase() {
    abstract fun soundLabDao(): SoundLabDao

    companion object {
        @Volatile
        private var INSTANCE: SoundLabDatabase? = null

        fun getDatabase(context: Context): SoundLabDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SoundLabDatabase::class.java,
                    "sound_lab_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
