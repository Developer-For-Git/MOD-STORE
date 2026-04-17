package com.sorwe.store.data.remote

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AppEntryDto(
    val id: String? = null,

    val name: String? = null,
    val description: String? = null,
    val icon: String? = null,

    val version: String? = null,
    val latestVersion: String? = null,

    val downloadUrl: String? = null,
    val repoUrl: String? = null,
    val githubRepo: String? = null,
    val releaseKeyword: String? = null,

    val packageName: String? = null,
    val category: String? = null,
    val platform: String? = null,
    val size: String? = null,
    val author: String? = null,

    val screenshots: List<String>? = null,

    // New fields from updated apps.json schema
    val gitlabRepo: String? = null,
    val gitlabDomain: String? = null,
    val officialSite: String? = null,

    // Legacy/internal fields (optional backward compatibility)
    val type: String = "github",
    val featured: Boolean = false,
    val customName: String? = null,
    val customDescription: String? = null
)
