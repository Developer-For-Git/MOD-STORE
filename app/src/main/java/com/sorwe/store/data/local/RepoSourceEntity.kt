package com.sorwe.store.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "repos")
data class RepoSourceEntity(
    @PrimaryKey
    val url: String,
    val name: String,
    val isEnabled: Boolean = true
)
