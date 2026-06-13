package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.AppDao
import com.example.data.model.*

@Database(
    entities = [
        UserProfile::class,
        Expense::class,
        Task::class,
        Project::class,
        Note::class,
        ChatMessage::class
    ],
    version = 2,
    exportSchema = false
)
abstract class KaelenDatabase : RoomDatabase() {
    abstract val appDao: AppDao

    companion object {
        @Volatile
        private var INSTANCE: KaelenDatabase? = null

        fun getDatabase(context: Context): KaelenDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KaelenDatabase::class.java,
                    "kaelen_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
