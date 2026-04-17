package com.sorwe.store.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * DTO for the release_notes.json on GitHub.
 */
@JsonClass(generateAdapter = true)
data class ReleaseNotesDto(
    val version: String,
    val date: String,
    val title: String,
    val changes: List<String>,
    @Json(name = "download_url") val downloadUrl: String
)
