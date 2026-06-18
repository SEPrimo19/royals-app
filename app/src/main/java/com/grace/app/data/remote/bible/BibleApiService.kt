package com.grace.app.data.remote.bible

import com.grace.app.data.remote.bible.dto.BibleApiResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface BibleApiService {
    @GET("{reference}")
    suspend fun getVerse(
        @Path("reference", encoded = true) reference: String,
        @Query("translation") translation: String = BibleTranslations.KJV
    ): BibleApiResponse
}

object BibleTranslations {
    const val KJV = "kjv"
    const val WEB = "web"
    const val ASV = "asv"
    const val BBE = "bbe"

}
