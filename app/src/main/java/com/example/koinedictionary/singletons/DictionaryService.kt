package com.example.koinedictionary.singletons

import android.content.Context
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
     * Retrieves all entries from the 'entries' table.
     */
    fun getAllEntries(context: Context): List<DictionaryEntry> {
        val db = getDatabase(context)
        val entries = mutableListOf<DictionaryEntry>()

        // Note: table name is 'greekDictionaryEntry'. Adjust if it differs in your sqlite file.
        val cursor = db.rawQuery("SELECT * FROM greekDictionaryEntry", null)
        cursor.use {
            val idIndex = it.getColumnIndex("id")
            val difficultyIndex = it.getColumnIndex("difficulty")
            val frequencyIndex = it.getColumnIndex("frequency")
            val fullWordIndex = it.getColumnIndex("fullWord")
            val glossIndex = it.getColumnIndex("gloss")
            val keyLetterIndex = it.getColumnIndex("keyLetter")
            val sourceNameIndex = it.getColumnIndex("sourceName")
            val typeIndex = it.getColumnIndex("type")
            val verbStemIndex = it.getColumnIndex("verbStem")
            val wordIndex = it.getColumnIndex("word")

            while (it.moveToNext()) {
                entries.add(
                    DictionaryEntry(
                        id = if (idIndex != -1) it.getInt(idIndex) else 0,
                        difficulty = if (difficultyIndex != -1) it.getInt(difficultyIndex) else 0,
                        frequency = if (frequencyIndex != -1) it.getInt(frequencyIndex) else 0,
                        fullWord = if (fullWordIndex != -1) it.getString(fullWordIndex) else "",
                        gloss = if (glossIndex != -1) it.getString(glossIndex) else "",
                        keyLetter = if (keyLetterIndex != -1) it.getString(keyLetterIndex) else "",
                        sourceName = if (sourceNameIndex != -1) it.getString(sourceNameIndex) else "",
                        type = if (typeIndex != -1) it.getString(typeIndex) else "",
                        verbStem = if (verbStemIndex != -1) it.getString(verbStemIndex) else "",
                        word = if (wordIndex != -1) it.getString(wordIndex) else ""
                    )
                )
            }
        }
        return entries
    }
}
