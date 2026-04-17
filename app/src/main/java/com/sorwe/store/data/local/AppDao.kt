package com.sorwe.store.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    @Query("SELECT * FROM apps ORDER BY addedAt DESC")
    fun getAllApps(): Flow<List<AppItemEntity>>

    @Query("SELECT * FROM apps WHERE isFavorite = 1 ORDER BY addedAt DESC")
    fun getFavoriteApps(): Flow<List<AppItemEntity>>

    @Query("SELECT * FROM apps WHERE id = :id LIMIT 1")
    suspend fun getAppById(id: String): AppItemEntity?

    @Query("SELECT * FROM apps WHERE id = :id LIMIT 1")
    fun getAppByIdFlow(id: String): Flow<AppItemEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApps(apps: List<AppItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: AppItemEntity)

    @Query("UPDATE apps SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: String, isFavorite: Boolean)

    @Query("SELECT isFavorite FROM apps WHERE id = :id")
    suspend fun isFavorite(id: String): Boolean?

    @Query("DELETE FROM apps WHERE isFavorite = 0")
    suspend fun clearNonFavoriteApps()

    @Query("DELETE FROM apps")
    suspend fun clearAll()

    /**
     * Returns IDs of apps whose cache is older than the given cutoff time.
     */
    @Query("SELECT id FROM apps WHERE lastUpdated < :cutoff")
    suspend fun getStaleAppIds(cutoff: Long): List<String>

    /**
     * Returns IDs of apps currently cached.
     */
    @Query("SELECT id FROM apps")
    suspend fun getAllAppIds(): List<String>

    /**
     * Returns IDs of favorited apps.
     */
    @Query("SELECT id FROM apps WHERE isFavorite = 1")
    suspend fun getFavoriteAppIds(): List<String>

    @Query("SELECT * FROM apps ORDER BY updateCount DESC LIMIT :limit")
    fun getMostUpdatedApps(limit: Int): Flow<List<AppItemEntity>>

    @Query("SELECT * FROM apps ORDER BY addedAt DESC LIMIT :limit")
    fun getNewestApps(limit: Int): Flow<List<AppItemEntity>>
}
