package com.grace.app.domain.repository

import com.grace.app.domain.model.BibleBook
import com.grace.app.domain.model.BibleSearchResult
import com.grace.app.domain.model.BibleVerse

interface BibleRepository {
    suspend fun getBooks(): List<BibleBook>

    suspend fun getVerses(bookOrder: Int, chapter: Int): List<BibleVerse>

    suspend fun searchVerses(query: String, limit: Int = 100): List<BibleSearchResult>
}
