package com.notebook.notes.checklist.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.notebook.notes.checklist.model.TodoItem
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoItemDao {

    @Query("SELECT * FROM todo_items WHERE listId = :listId ORDER BY position ASC")
    fun getItemsForList(listId: Long): Flow<List<TodoItem>>

    @Query("SELECT * FROM todo_items WHERE listId = :listId ORDER BY position ASC")
    suspend fun getItemsForListOnce(listId: Long): List<TodoItem>

    @Query("SELECT * FROM todo_items WHERE id = :itemId")
    suspend fun getItemById(itemId: Long): TodoItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: TodoItem): Long

    @Update
    suspend fun updateItem(item: TodoItem)

    @Query("DELETE FROM todo_items WHERE id = :itemId")
    suspend fun deleteItem(itemId: Long)

    @Query("SELECT COUNT(*) FROM todo_items WHERE listId = :listId")
    suspend fun getItemCount(listId: Long): Int

    @Query("SELECT COUNT(*) FROM todo_items WHERE listId = :listId AND isCompleted = 1")
    suspend fun getCompletedCount(listId: Long): Int

    @Query(
        "SELECT * FROM todo_items WHERE listId = :listId AND text LIKE '%' || :query || '%' ORDER BY position ASC"
    )
    fun searchItems(listId: Long, query: String): Flow<List<TodoItem>>

    @Query(
        "UPDATE todo_items SET position = position + :delta WHERE listId = :listId AND position >= :fromPosition"
    )
    suspend fun shiftPositionsFrom(listId: Long, fromPosition: Int, delta: Int)
}
