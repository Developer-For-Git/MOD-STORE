package com.sorwe.store.data.model

data class AppItem(
    val id: String,
    val name: String,
    val description: String,
    val version: String,
    val size: String,
    val category: String,
    val icon: String,
    val screenshots: List<String>,
    val downloadUrl: String,
    val changelog: String,
    val developer: String,
    val rating: Double = 0.0,
    val featured: Boolean = false,
    val banner: String = "",
    val sourceType: String = "",
    val platform: String = "Android",
    val repoUrl: String = "",
    val releaseKeyword: String = "",
    val addedAt: Long = 0L,
    val updateCount: Int = 0
)
