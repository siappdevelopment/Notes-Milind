package com.notebook.notes.checklist.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.notebook.notes.checklist.model.TodoList
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoListDao {

    @Query("SELECT * FROM todo_lists WHERE id = :listId")
    suspend fun getListById(listId: Long): TodoList?

    @Query("SELECT * FROM todo_lists WHERE noteId = :noteId LIMIT 1")
    suspend fun getListByNoteId(noteId: Long): TodoList?

    @Query("SELECT * FROM todo_lists ORDER BY updatedAt DESC")
    fun getAllLists(): Flow<List<TodoList>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(list: TodoList): Long

    @Update
    suspend fun updateList(list: TodoList)

    @Query("UPDATE todo_lists SET noteId = :noteId, updatedAt = :updatedAt WHERE id = :listId")
    suspend fun linkToNote(listId: Long, noteId: Long, updatedAt: Long)

    @Query("UPDATE todo_lists SET title = :title, updatedAt = :updatedAt WHERE id = :listId")
    suspend fun updateTitle(listId: Long, title: String, updatedAt: Long)
}
