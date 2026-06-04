package com.grace.app.data.remote.bible

import com.grace.app.data.remote.bible.dto.BibleApiResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface BibleApiService {
    /**
     * Fetch a passage by reference. The [reference] is encoded into the URL
     * path; bible-api.com accepts "John 3:16" (URL-encoded). NKJV is not
     * available here — see [BibleTranslations] for what is.
     */
    @GET("{reference}")
    suspend fun getVerse(
        @Path("reference", encoded = true) reference: String,
        @Query("translation") translation: String = BibleTranslations.KJV
    ): BibleApiResponse
}

/**
 * Translation IDs accepted by bible-api.com. KJV is the default because it is
 * the highest-quality public-domain translation. Switch the constant below if
 * you later add a Settings selector.
 */
object BibleTranslations {
    const val KJV = "kjv"
    const val WEB = "web"   // World English Bible — modern public domain
    const val ASV = "asv"   // American Standard Version
    const val BBE = "bbe"   // Bible in Basic English

    // For NKJV you'll need a licensed provider (e.g. scripture.api.bible).
    // Keeping this comment so future work doesn't waste time hunting for it.
}
