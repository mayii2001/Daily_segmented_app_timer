package com.apptimer.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.apptimer.database.entities.AppLimit
import com.apptimer.database.entities.UsageHistory

@Database(
    entities = [AppLimit::class, UsageHistory::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun appLimitDao(): AppLimitDao
    abstract fun usageHistoryDao(): UsageHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS app_limits_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        packageName TEXT NOT NULL,
                        appName TEXT NOT NULL,
                        timeLimit INTEGER NOT NULL,
                        enabled INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    INSERT INTO app_limits_new (packageName, appName, timeLimit, enabled)
                    SELECT src.packageName, src.appName, src.timeLimit, src.enabled
                    FROM app_limits AS src
                    INNER JOIN (
                        SELECT packageName, MAX(id) AS maxId
                        FROM app_limits
                        GROUP BY packageName
                    ) AS dedup
                    ON src.packageName = dedup.packageName
                    AND src.id = dedup.maxId
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_app_limits_packageName ON app_limits_new(packageName)"
                )
                database.execSQL("DROP TABLE app_limits")
                database.execSQL("ALTER TABLE app_limits_new RENAME TO app_limits")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_timer_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
