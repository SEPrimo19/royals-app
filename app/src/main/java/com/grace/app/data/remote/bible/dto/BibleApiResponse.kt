package com.grace.app.data.remote.bible.dto

import com.google.gson.annotations.SerializedName

/**
 * bible-api.com response shape. Free, no auth, supports KJV (and a few other
 * public-domain translations: WEB, ASV, BBE). NKJV is licensed and not here.
 *
 * Example: GET https://bible-api.com/john%203:16?translation=kjv
 * {
 *   "reference": "John 3:16",
 *   "text": "For God so loved the world...\n",
 *   "translation_id": "kjv",
 *   "translation_name": "King James Version"
 * }
 */
data class BibleApiResponse(
    @SerializedName("reference") val reference: String = "",
    @SerializedName("text") val text: String = "",
    @SerializedName("translation_id") val translationId: String = "",
    @SerializedName("translation_name") val translationName: String = ""
)
