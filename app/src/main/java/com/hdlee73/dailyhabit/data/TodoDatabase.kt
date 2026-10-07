package com.hdlee73.dailyhabit.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "todos")
data class Todo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val done: Boolean = false,
    /** 마감일 (epochDay), 없으면 null */
    val dueEpochDay: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

@Dao
interface TodoDao {
    @Query("SELECT * FROM todos ORDER BY done ASC, CASE WHEN dueEpochDay IS NULL THEN 1 ELSE 0 END, dueEpochDay ASC, createdAt DESC")
    fun observeAll(): Flow<List<Todo>>

    @Query("SELECT * FROM todos WHERE done = 0 ORDER BY CASE WHEN dueEpochDay IS NULL THEN 1 ELSE 0 END, dueEpochDay ASC, createdAt DESC")
    suspend fun pending(): List<Todo>

    @Insert
    suspend fun insert(todo: Todo): Long

    @Update
    suspend fun update(todo: Todo)

    @Delete
    suspend fun delete(todo: Todo)

    @Query("DELETE FROM todos WHERE done = 1")
    suspend fun clearDone()
}

@Database(entities = [Todo::class], version = 1, exportSchema = false)
abstract class TodoDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDao

    companion object {
        @Volatile private var instance: TodoDatabase? = null

        fun get(context: Context): TodoDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, TodoDatabase::class.java, "dailyhabit.db")
                .build().also { instance = it }
        }
    }
}
