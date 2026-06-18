package com.grace.app.data.remote.bible.dto

import com.google.gson.annotations.SerializedName

data class BibleApiResponse(
    @SerializedName("reference") val reference: String = "",
    @SerializedName("text") val text: String = "",
    @SerializedName("translation_id") val translationId: String = "",
    @SerializedName("translation_name") val translationName: String = ""
)
