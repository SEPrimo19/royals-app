package com.grace.app.data.local.bible

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.grace.app.domain.model.BibleBook
import com.grace.app.domain.model.BibleSearchResult
import com.grace.app.domain.model.BibleVerse
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BibleLocalDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    @Volatile private var db: SQLiteDatabase? = null

    private fun open(): SQLiteDatabase {
        db?.let { return it }
        return synchronized(this) {
            db ?: run {
                val target = context.getDatabasePath(DB_NAME)
                if (!target.exists()) {
                    target.parentFile?.mkdirs()
                    context.assets.open(DB_NAME).use { input ->
                        target.outputStream().use { output -> input.copyTo(output) }
                    }
                }
                SQLiteDatabase.openDatabase(
                    target.path, null, SQLiteDatabase.OPEN_READONLY
                ).also { db = it }
            }
        }
    }

    fun books(): List<BibleBook> {
        val out = ArrayList<BibleBook>(66)
        open().rawQuery(
            "SELECT book_order, name, testament, chapter_count FROM books ORDER BY book_order",
            null
        ).use { c ->
            while (c.moveToNext()) {
                out += BibleBook(
                    order = c.getInt(0),
                    name = c.getString(1),
                    testament = c.getString(2),
                    chapterCount = c.getInt(3)
                )
            }
        }
        return out
    }

    fun verses(bookOrder: Int, chapter: Int): List<BibleVerse> {
        val out = ArrayList<BibleVerse>()
        open().rawQuery(
            "SELECT verse, text FROM verses WHERE book_order = ? AND chapter = ? ORDER BY verse",
            arrayOf(bookOrder.toString(), chapter.toString())
        ).use { c ->
            while (c.moveToNext()) {
                out += BibleVerse(verse = c.getInt(0), text = c.getString(1))
            }
        }
        return out
    }

    fun search(query: String, limit: Int = 100): List<BibleSearchResult> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val out = ArrayList<BibleSearchResult>()
        open().rawQuery(
            """
            SELECT v.book_order, b.name, v.chapter, v.verse, v.text
              FROM verses v
              JOIN books b ON b.book_order = v.book_order
             WHERE v.text LIKE ? COLLATE NOCASE
             ORDER BY v.book_order, v.chapter, v.verse
             LIMIT ?
            """.trimIndent(),
            arrayOf("%$q%", limit.toString())
        ).use { c ->
            while (c.moveToNext()) {
                out += BibleSearchResult(
                    bookOrder = c.getInt(0),
                    bookName = c.getString(1),
                    chapter = c.getInt(2),
                    verse = c.getInt(3),
                    text = c.getString(4)
                )
            }
        }
        return out
    }

    companion object {
        private const val DB_NAME = "kjv.db"
    }
}
