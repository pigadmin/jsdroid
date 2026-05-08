package com.jiuzhuan.jsdroid.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface LogDao {
    @Query("SELECT * FROM logs ORDER BY timestamp DESC LIMIT 5000")
    suspend fun getALL(): List<LogEntity>

    @Query("SELECT * FROM logs WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getByTimeRange(startTime: Long, endTime: Long, limit: Int, offset: Int = 0): List<LogEntity>

    @Query("SELECT COUNT(*) FROM logs WHERE timestamp BETWEEN :startTime AND :endTime")
    suspend fun countByTimeRange(startTime: Long, endTime: Long): Int

    @Insert
    suspend fun insert(log: LogEntity)

    @Query("DELETE FROM logs")
    suspend fun delete()
}