package com.sorwe.store.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RepoDao {
    @Query("SELECT * FROM repos")
    fun getAllReposFlow(): Flow<List<RepoSourceEntity>>

    @Query("SELECT * FROM repos")
    suspend fun getAllRepos(): List<RepoSourceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepo(repo: RepoSourceEntity)

    @Delete
    suspend fun deleteRepo(repo: RepoSourceEntity)
}
