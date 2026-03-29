package com.example.koinedictionary.singletons

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.example.koinedictionary.data.models.DictionaryEntry
import java.io.File
import java.io.FileOutputStream

object DictionaryService {
    private const val DATABASE_NAME = "pocketGreekEntries.sqlite"
    private var database: SQLiteDatabase? = null

    /**
     * Opens the database. If it doesn't exist in the app's internal storage, 
     * it copies it from the assets folder.
     */
    fun getDatabase(context: Context): SQLiteDatabase {
        if (database == null || !database!!.isOpen) {
            val dbFile = context.getDatabasePath(DATABASE_NAME)
            if (!dbFile.exists()) {
                dbFile.parentFile?.mkdirs()
                copyDatabaseFromAssets(context, dbFile)
            }
            database = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY)
        }
        return database!!
    }

    private fun copyDatabaseFromAssets(context: Context, dbFile: File) {
        context.assets.open(DATABASE_NAME).use { inputStream ->
            FileOutputStream(dbFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }

    /**
     * Retrieves all entries from the 'greekDictionaryEntry' table.
     */
    fun getAllEntries(context: Context): List<DictionaryEntry> {
        val db = getDatabase(context)
        return db.rawQuery("SELECT * FROM greekDictionaryEntry", null).use { cursor ->
            mapCursorToEntries(cursor)
        }
    }

    /**
     * Retrieves all entries from the 'greekDictionaryEntry' table that match the given key letter.
     */
    fun getEntriesByKeyLetter(context: Context, keyLetter: String): List<DictionaryEntry> {
        val db = getDatabase(context)
        return db.rawQuery(
            "SELECT * FROM greekDictionaryEntry WHERE keyLetter = ?",
            arrayOf(keyLetter)
        ).use { cursor ->
            mapCursorToEntries(cursor)
        }
    }

    private fun mapCursorToEntries(cursor: Cursor): List<DictionaryEntry> {
        val entries = mutableListOf<DictionaryEntry>()
        val idIndex = cursor.getColumnIndex("id")
        val difficultyIndex = cursor.getColumnIndex("difficulty")
        val frequencyIndex = cursor.getColumnIndex("frequency")
        val fullWordIndex = cursor.getColumnIndex("fullWord")
        val glossIndex = cursor.getColumnIndex("gloss")
        val keyLetterIndex = cursor.getColumnIndex("keyLetter")
        val sourceNameIndex = cursor.getColumnIndex("sourceName")
        val typeIndex = cursor.getColumnIndex("type")
        val verbStemIndex = cursor.getColumnIndex("verbStem")
        val wordIndex = cursor.getColumnIndex("word")

        while (cursor.moveToNext()) {
            entries.add(
                DictionaryEntry(
                    id = if (idIndex != -1) cursor.getInt(idIndex) else 0,
                    difficulty = if (difficultyIndex != -1) cursor.getInt(difficultyIndex) else 0,
                    frequency = if (frequencyIndex != -1) cursor.getInt(frequencyIndex) else 0,
                    fullWord = if (fullWordIndex != -1) cursor.getString(fullWordIndex) else "",
                    gloss = if (glossIndex != -1) cursor.getString(glossIndex) else "",
                    keyLetter = if (keyLetterIndex != -1) cursor.getString(keyLetterIndex) else "",
                    sourceName = if (sourceNameIndex != -1) cursor.getString(sourceNameIndex) else "",
                    type = if (typeIndex != -1) cursor.getString(typeIndex) else "",
                    verbStem = if (verbStemIndex != -1) cursor.getString(verbStemIndex) else "",
                    word = if (wordIndex != -1) cursor.getString(wordIndex) else ""
                )
            )
        }
        return entries
    }
}
