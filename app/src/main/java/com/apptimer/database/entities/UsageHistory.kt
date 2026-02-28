package com.apptimer.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usage_history")
data class UsageHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val startTime: Long, // 开始时间戳（毫秒）
    val endTime: Long,   // 结束时间戳（毫秒）
    val duration: Long   // 使用时长（秒）
)
