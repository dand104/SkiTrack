package org.skitrace.skitrace.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import org.skitrace.skitrace.data.db.dao.TrackDao
import org.skitrace.skitrace.data.db.entity.TrackPointEntity
import org.skitrace.skitrace.data.db.entity.TrackRunEntity

@Database(
    entities = [TrackRunEntity::class, TrackPointEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SkiDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao

    companion object {
        @Volatile
        private var INSTANCE: SkiDatabase? = null

        fun getDatabase(context: Context): SkiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SkiDatabase::class.java,
                    "skitrace_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}