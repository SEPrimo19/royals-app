package com.grace.app.data.remote.supabase.dto

import com.grace.app.domain.model.AppVersion
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppVersionDto(
    @SerialName("version_code")   val versionCode: Int,
    @SerialName("version_name")   val versionName: String,
    @SerialName("download_url")   val downloadUrl: String,
    @SerialName("release_notes")  val releaseNotes: String? = null,
    @SerialName("is_mandatory")   val isMandatory: Boolean = false
)

fun AppVersionDto.toDomain() = AppVersion(
    versionCode = versionCode,
    versionName = versionName,
    downloadUrl = downloadUrl,
    releaseNotes = releaseNotes,
    isMandatory = isMandatory
)
