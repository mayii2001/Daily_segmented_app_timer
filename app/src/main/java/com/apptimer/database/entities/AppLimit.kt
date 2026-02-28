package com.apptimer.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "app_limits",
    indices = [Index(value = ["packageName"], unique = true)]
)
data class AppLimit(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val appName: String,
    val timeLimit: Long, // 时间限制（秒）
    val enabled: Boolean = true
)
