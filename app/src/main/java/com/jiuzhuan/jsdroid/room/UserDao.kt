package com.jiuzhuan.jsdroid.room

import androidx.room.Dao
import androidx.room.Insert

@Dao
interface UserDao {
    @Insert
    suspend fun insert(log: LogEntity)
}