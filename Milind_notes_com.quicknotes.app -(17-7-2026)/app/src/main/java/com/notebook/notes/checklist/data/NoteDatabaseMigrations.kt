package com.notebook.notes.checklist.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE notes ADD COLUMN position INTEGER NOT NULL DEFAULT 0")

        val cursor = db.query(
            "SELECT id FROM notes ORDER BY isPinned DESC, updatedAt DESC, createdAt ASC"
        )
        var position = 0L
        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(0)
                db.execSQL("UPDATE notes SET position = ? WHERE id = ?", arrayOf(position, id))
                position++
            }
        }
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE notes ADD COLUMN reminderEnabled INTEGER NOT NULL DEFAULT 0"
        )
        db.execSQL(
            "ALTER TABLE notes ADD COLUMN reminderTimeMillis INTEGER NOT NULL DEFAULT 0"
        )
        db.execSQL(
            "ALTER TABLE notes ADD COLUMN repeatType TEXT NOT NULL DEFAULT 'NEVER'"
        )
        db.execSQL(
            "ALTER TABLE notes ADD COLUMN notificationId INTEGER NOT NULL DEFAULT 0"
        )
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE notes ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0"
        )
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE notes ADD COLUMN listItemHeight INTEGER")
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE notes ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0"
        )
        db.execSQL(
            "ALTER TABLE notes ADD COLUMN deletedAt INTEGER NOT NULL DEFAULT 0"
        )
    }
}
