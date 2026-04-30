package com.jiuzhuan.jsdroid.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface LogDao {
    @Query("SELECT * FROM logs")
    suspend fun getALL(): List<LogEntity>

    @Query("SELECT * FROM logs WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getByDate(start: Long, end: Long, limit: Int, offset: Int = 1): List<LogEntity>

    @Insert
    suspend fun insert(log: LogEntity)

    @Query("DELETE FROM logs")
    suspend fun delete()
}