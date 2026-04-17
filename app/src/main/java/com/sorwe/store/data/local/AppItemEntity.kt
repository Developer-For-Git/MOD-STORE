package com.sorwe.store.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sorwe.store.data.model.AppItem

@Entity(tableName = "apps")
data class AppItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val version: String,
    val size: String,
    val category: String,
    val icon: String,
    val screenshots: String, // Stored as delimiter-separated string
    val downloadUrl: String,
    val changelog: String,
    val developer: String,
    val isFavorite: Boolean = false,
    val rating: Double = 0.0,
    val featured: Boolean = false,
    val banner: String = "",
    val sourceType: String = "",
    val platform: String = "Android",
    val repoUrl: String = "",
    val releaseKeyword: String = "",
    val lastUpdated: Long = 0L,
    val addedAt: Long = 0L,
    val updateCount: Int = 0
) {
    fun toAppItem(): AppItem = AppItem(
        id = id,
        name = name,
        description = description,
        version = version,
        size = size,
        category = category,
        icon = icon,
        screenshots = if (screenshots.isBlank()) emptyList() else screenshots.split("|||"),
        downloadUrl = downloadUrl,
        changelog = changelog,
        developer = developer,
        rating = rating,
        featured = featured,
        banner = banner,
        sourceType = sourceType,
        platform = platform,
        repoUrl = repoUrl,
        releaseKeyword = releaseKeyword,
        addedAt = addedAt,
        updateCount = updateCount
    )

    companion object {
        fun fromAppItem(
            appItem: AppItem,
            isFavorite: Boolean = false,
            lastUpdated: Long = System.currentTimeMillis()
        ): AppItemEntity =
            AppItemEntity(
                id = appItem.id,
                name = appItem.name,
                description = appItem.description,
                version = appItem.version,
                size = appItem.size,
                category = appItem.category,
                icon = appItem.icon,
                screenshots = appItem.screenshots.joinToString("|||"),
                downloadUrl = appItem.downloadUrl,
                changelog = appItem.changelog,
                developer = appItem.developer,
                isFavorite = isFavorite,
                rating = appItem.rating,
                featured = appItem.featured,
                banner = appItem.banner,
                sourceType = appItem.sourceType,
                platform = appItem.platform,
                repoUrl = appItem.repoUrl,
                releaseKeyword = appItem.releaseKeyword,
                lastUpdated = lastUpdated,
                addedAt = appItem.addedAt,
                updateCount = appItem.updateCount
            )
    }
}
