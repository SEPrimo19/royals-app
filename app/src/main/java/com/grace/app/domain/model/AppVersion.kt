package com.grace.app.domain.model

data class AppVersion(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String,
    val releaseNotes: String?,
    val isMandatory: Boolean
)
