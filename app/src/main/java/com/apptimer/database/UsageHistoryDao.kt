package com.apptimer.database

import androidx.room.*
import com.apptimer.database.entities.UsageHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageHistoryDao {

    @Query("SELECT * FROM usage_history ORDER BY startTime DESC")
    fun getAllHistory(): Flow<List<UsageHistory>>

    @Query("SELECT * FROM usage_history WHERE packageName = :packageName ORDER BY startTime DESC")
    fun getHistoryByPackage(packageName: String): Flow<List<UsageHistory>>

    @Query("SELECT * FROM usage_history WHERE startTime >= :startTime AND endTime <= :endTime")
    suspend fun getHistoryInRange(startTime: Long, endTime: Long): List<UsageHistory>

    @Query("SELECT SUM(duration) FROM usage_history WHERE packageName = :packageName AND startTime >= :startTime")
    suspend fun getTotalDurationToday(packageName: String, startTime: Long): Long?

    @Query("SELECT SUM(duration) FROM usage_history WHERE packageName = :packageName AND startTime >= :startTime")
    suspend fun getTotalDurationSince(packageName: String, startTime: Long): Long?

    @Insert
    suspend fun insertHistory(history: UsageHistory)

    @Delete
    suspend fun deleteHistory(history: UsageHistory)

    @Query("DELETE FROM usage_history WHERE startTime < :timestamp")
    suspend fun deleteOldHistory(timestamp: Long)
}
