package com.notebook.notes.checklist.di

import android.content.Context
import androidx.room.Room
import com.notebook.notes.checklist.data.AttachmentDao
import com.notebook.notes.checklist.data.CategoryDao
import com.notebook.notes.checklist.data.NoteBlockDao
import com.notebook.notes.checklist.data.NoteDao
import com.notebook.notes.checklist.data.NoteDatabase
import com.notebook.notes.checklist.data.MIGRATION_5_6
import com.notebook.notes.checklist.data.MIGRATION_6_7
import com.notebook.notes.checklist.data.MIGRATION_7_8
import com.notebook.notes.checklist.data.MIGRATION_8_9
import com.notebook.notes.checklist.data.MIGRATION_9_10
import com.notebook.notes.checklist.data.ReminderDao
import com.notebook.notes.checklist.data.TodoItemDao
import com.notebook.notes.checklist.data.TodoListDao
import com.notebook.notes.checklist.helper.PreferenceHelper
import com.google.gson.Gson
import com.notebook.notes.checklist.ads.AppSharedPref
import com.notebook.notes.checklist.ads.AppSharedPreferencesApp
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NoteDatabase {
        return Room.databaseBuilder(
            context,
            NoteDatabase::class.java,
            "notes_db"
        ).addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideCategoryDao(database: NoteDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideNoteDao(database: NoteDatabase): NoteDao = database.noteDao()

    @Provides
    fun provideNoteBlockDao(database: NoteDatabase): NoteBlockDao = database.noteBlockDao()

    @Provides
    fun provideAttachmentDao(database: NoteDatabase): AttachmentDao = database.attachmentDao()

    @Provides
    fun provideReminderDao(database: NoteDatabase): ReminderDao = database.reminderDao()

    @Provides
    fun provideTodoListDao(database: NoteDatabase): TodoListDao = database.todoListDao()

    @Provides
    fun provideTodoItemDao(database: NoteDatabase): TodoItemDao = database.todoItemDao()

    @Provides
    @Singleton
    fun providePreferenceHelper(@ApplicationContext context: Context): PreferenceHelper {
        return PreferenceHelper(context)
    }

    @Singleton
    @Provides
    fun provideSharedPref(
        @ApplicationContext context: Context
    ): AppSharedPref = AppSharedPreferencesApp(context)

}
