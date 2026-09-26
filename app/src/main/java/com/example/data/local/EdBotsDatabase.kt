package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.AutoReplyDao
import com.example.data.local.dao.BotDao
import com.example.data.local.dao.CommandDao
import com.example.data.local.entity.AutoReplyEntity
import com.example.data.local.entity.BotEntity
import com.example.data.local.entity.CommandEntity

@Database(
    entities = [
        BotEntity::class,
        CommandEntity::class,
        AutoReplyEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class EdBotsDatabase : RoomDatabase() {
    abstract fun botDao(): BotDao
    abstract fun commandDao(): CommandDao
    abstract fun autoReplyDao(): AutoReplyDao

    companion object {
        @Volatile
        private var INSTANCE: EdBotsDatabase? = null

        fun getInstance(context: Context): EdBotsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EdBotsDatabase::class.java,
                    "edbots_database.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
