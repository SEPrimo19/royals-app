package com.grace.app.data.repository

import com.grace.app.data.local.bible.BibleLocalDataSource
import com.grace.app.domain.model.BibleBook
import com.grace.app.domain.model.BibleSearchResult
import com.grace.app.domain.model.BibleVerse
import com.grace.app.domain.repository.BibleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class BibleRepositoryImpl @Inject constructor(
    private val local: BibleLocalDataSource
) : BibleRepository {

    override suspend fun getBooks(): List<BibleBook> =
        withContext(Dispatchers.IO) { local.books() }

    override suspend fun getVerses(bookOrder: Int, chapter: Int): List<BibleVerse> =
        withContext(Dispatchers.IO) { local.verses(bookOrder, chapter) }

    override suspend fun searchVerses(query: String, limit: Int): List<BibleSearchResult> =
        withContext(Dispatchers.IO) { local.search(query, limit) }
}
