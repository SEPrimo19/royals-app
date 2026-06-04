package com.grace.app.data.remote.bible.dto

import com.google.gson.annotations.SerializedName

// ESV API passage/text response. Expanded/validated in Prompt 4.
data class VerseResponse(
    @SerializedName("query") val query: String = "",
    @SerializedName("passages") val passages: List<String> = emptyList()
)
