package com.hdlee73.dailyhabit.data

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

// ───────────── 할 일 ─────────────

enum class Priority(val label: String) { LOW("낮음"), NORMAL("보통"), HIGH("높음") }

val TODO_CATEGORIES = listOf("개인", "가정", "일", "신앙", "건강", "쇼핑")

@Entity(tableName = "todos")
data class Todo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val done: Boolean = false,
    /** 마감일 (epochDay) */
    val dueEpochDay: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    /** 0 낮음, 1 보통, 2 높음 */
    @ColumnInfo(defaultValue = "1") val priority: Int = 1,
    @ColumnInfo(defaultValue = "''") val category: String = "",
    @ColumnInfo(defaultValue = "''") val memo: String = "",
    /** 마감 시각 (자정부터 분), 없으면 종일 */
    val dueMinute: Int? = null,
    /** 마감 시각에 알림 */
    @ColumnInfo(defaultValue = "0") val remind: Boolean = false,
    val completedAt: Long? = null,
) {
    val priorityEnum: Priority get() = Priority.entries.getOrElse(priority) { Priority.NORMAL }
}

@Dao
interface TodoDao {
    @Query("SELECT * FROM todos ORDER BY done ASC, CASE WHEN dueEpochDay IS NULL THEN 1 ELSE 0 END, dueEpochDay ASC, priority DESC, createdAt DESC")
    fun observeAll(): Flow<List<Todo>>

    @Query("SELECT * FROM todos WHERE done = 0 ORDER BY CASE WHEN dueEpochDay IS NULL THEN 1 ELSE 0 END, dueEpochDay ASC, priority DESC, createdAt DESC")
    suspend fun pending(): List<Todo>

    @Query("SELECT * FROM todos WHERE id = :id")
    suspend fun byId(id: Long): Todo?

    @Query("SELECT * FROM todos WHERE done = 0 AND remind = 1 AND dueEpochDay IS NOT NULL AND dueMinute IS NOT NULL")
    suspend fun withReminders(): List<Todo>

    @Insert
    suspend fun insert(todo: Todo): Long

    @Update
    suspend fun update(todo: Todo)

    @Delete
    suspend fun delete(todo: Todo)

    @Query("DELETE FROM todos WHERE done = 1")
    suspend fun clearDone()
}

// ───────────── 매일 루틴 ─────────────

@Entity(tableName = "routines")
data class Routine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val emoji: String = "✨",
    /** RoutinePalette 인덱스 */
    val color: Int = 0,
    /** 반복 요일 비트마스크 (월=1, 화=2, … 일=64) */
    val days: Int = ALL_DAYS,
    val reminderEnabled: Boolean = false,
    /** 알림 시각 (자정부터 분) */
    val reminderMinute: Int = 8 * 60,
    val createdEpochDay: Long,
    val archived: Boolean = false,
    val sortOrder: Int = 0,
) {
    fun activeOn(dayOfWeek: java.time.DayOfWeek): Boolean = days and (1 shl (dayOfWeek.value - 1)) != 0

    companion object {
        const val ALL_DAYS = 0b1111111
    }
}

@Entity(tableName = "routine_checks", primaryKeys = ["routineId", "epochDay"])
data class RoutineCheck(
    val routineId: Long,
    val epochDay: Long,
    val checkedAt: Long = System.currentTimeMillis(),
)

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routines WHERE archived = 0 ORDER BY sortOrder ASC, id ASC")
    fun observeActive(): Flow<List<Routine>>

    @Query("SELECT * FROM routines WHERE archived = 0 ORDER BY sortOrder ASC, id ASC")
    suspend fun active(): List<Routine>

    @Query("SELECT * FROM routines WHERE id = :id")
    suspend fun byId(id: Long): Routine?

    @Query("SELECT * FROM routine_checks")
    fun observeChecks(): Flow<List<RoutineCheck>>

    @Query("SELECT COUNT(*) FROM routine_checks WHERE routineId = :routineId AND epochDay = :epochDay")
    suspend fun isChecked(routineId: Long, epochDay: Long): Int

    @Query("SELECT * FROM routine_checks WHERE epochDay = :epochDay")
    suspend fun checksOn(epochDay: Long): List<RoutineCheck>

    @Insert
    suspend fun insert(routine: Routine): Long

    @Update
    suspend fun update(routine: Routine)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun check(check: RoutineCheck)

    @Query("DELETE FROM routine_checks WHERE routineId = :routineId AND epochDay = :epochDay")
    suspend fun uncheck(routineId: Long, epochDay: Long)

    @Query("DELETE FROM routines WHERE id = :id")
    suspend fun deleteRoutine(id: Long)

    @Query("DELETE FROM routine_checks WHERE routineId = :id")
    suspend fun deleteChecks(id: Long)

    @Transaction
    suspend fun delete(id: Long) {
        deleteChecks(id)
        deleteRoutine(id)
    }

    @Transaction
    suspend fun toggle(routineId: Long, epochDay: Long) {
        if (isChecked(routineId, epochDay) > 0) uncheck(routineId, epochDay)
        else check(RoutineCheck(routineId, epochDay))
    }
}

// ───────────── DB ─────────────

@Database(entities = [Todo::class, Routine::class, RoutineCheck::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDao
    abstract fun routineDao(): RoutineDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE todos ADD COLUMN priority INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE todos ADD COLUMN category TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE todos ADD COLUMN memo TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE todos ADD COLUMN dueMinute INTEGER")
                db.execSQL("ALTER TABLE todos ADD COLUMN remind INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE todos ADD COLUMN completedAt INTEGER")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `routines` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`title` TEXT NOT NULL, `emoji` TEXT NOT NULL, `color` INTEGER NOT NULL, `days` INTEGER NOT NULL, " +
                        "`reminderEnabled` INTEGER NOT NULL, `reminderMinute` INTEGER NOT NULL, " +
                        "`createdEpochDay` INTEGER NOT NULL, `archived` INTEGER NOT NULL, `sortOrder` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `routine_checks` (`routineId` INTEGER NOT NULL, `epochDay` INTEGER NOT NULL, " +
                        "`checkedAt` INTEGER NOT NULL, PRIMARY KEY(`routineId`, `epochDay`))"
                )
            }
        }

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "dailyhabit.db")
                .addMigrations(MIGRATION_1_2)
                .build().also { instance = it }
        }
    }
}
