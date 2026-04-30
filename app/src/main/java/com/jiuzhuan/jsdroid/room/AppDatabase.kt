package com.jiuzhuan.jsdroid.room

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.jiuzhuan.jsdroid.api.getApp

@Database(entities = [UserEntity::class, LogEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun logDao(): LogDao
    abstract fun userDao(): UserDao

    companion object {
        private var instance: AppDatabase? = null

        fun getInstance(): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    getApp(), AppDatabase::class.java, "jz.db"
                )
//                    .setJournalMode(JournalMode.TRUNCATE)   //debug
//                    .setQueryCallback({ sqlQuery, bindArgs ->
//                        logd(sqlQuery, bindArgs.joinToString())   //debug
//                    }, Executors.newSingleThreadExecutor())   //debug
                    .build().also { instance = it }
            }
        }
    }
}