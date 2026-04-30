package com.jiuzhuan.jsdroid.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

var level_nomal = "nomal"
var level_warn = "warn"
var level_success = "success"
var level_error = "error"

@Entity(tableName = "logs")
data class LogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "level") val level: String,
    @ColumnInfo(name = "message") val message: String
)