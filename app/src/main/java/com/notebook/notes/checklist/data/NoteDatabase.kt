package com.notebook.notes.checklist.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.notebook.notes.checklist.model.Attachment
import com.notebook.notes.checklist.model.Category
import com.notebook.notes.checklist.model.Note
import com.notebook.notes.checklist.model.NoteBlock
import com.notebook.notes.checklist.model.Reminder
import com.notebook.notes.checklist.model.TodoItem
import com.notebook.notes.checklist.model.TodoList

@Database(
    entities = [Note::class, NoteBlock::class, Reminder::class, TodoList::class, TodoItem::class, Attachment::class, Category::class],
    version = 10,
    exportSchema = false
)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun noteDao(): NoteDao
    abstract fun noteBlockDao(): NoteBlockDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun reminderDao(): ReminderDao
    abstract fun todoListDao(): TodoListDao
    abstract fun todoItemDao(): TodoItemDao
}
