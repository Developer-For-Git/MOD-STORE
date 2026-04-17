package com.sorwe.store.data.repository

import android.util.Log
import com.sorwe.store.data.local.AppDao
import com.sorwe.store.data.local.AppItemEntity
import com.sorwe.store.data.local.RepoSourceEntity
import com.sorwe.store.data.model.AppItem
import com.sorwe.store.data.model.Resource
import com.sorwe.store.data.remote.AppApiService
import com.sorwe.store.data.remote.AppEntryDto
import com.sorwe.store.data.remote.GitHubMetadataProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    private val apiService: AppApiService,
    private val appDao: AppDao,
    private val repoDao: com.sorwe.store.data.local.RepoDao,
    private val githubProvider: GitHubMetadataProvider
) {
    companion object {
        private const val CACHE_DURATION_MS = 2 * 60 * 60 * 1000L // 2 hours
        const val DEFAULT_REPO_URL =
            "https://raw.githubusercontent.com/Developer-For-Git/MOD-STORE-DATA-/refs/heads/main/apps.json"
    }

    fun getApps(forceRefresh: Boolean = false): Flow<Resource<List<AppItem>>> = flow {
        emit(Resource.Loading)

        // 1. Read cache
        val cachedEntities = try { appDao.getAllApps().first() } catch (e: Exception) { emptyList() }
        if (cachedEntities.isNotEmpty()) {
            emit(Resource.Success(cachedEntities.map { it.toAppItem() }))
        }

        // 2. Ensure default repo
        try { ensureDefaultRepo() } catch (e: Exception) { Log.e("AppRepository", "ensureDefaultRepo failed", e) }

        // 3. Fetch entries (Exceptions here will bubble up to .catch)
        val entries = fetchAllEntries()
        val entriesWithId = entries.filter { it.id != null }
        Log.d("AppRepository", "Fetched ${entries.size} total, ${entriesWithId.size} valid IDs")

        if (entriesWithId.isEmpty()) {
            if (cachedEntities.isEmpty()) {
                emit(Resource.Error("The app store is empty (0 valid entries)."))
            }
            return@flow
        }

        // 4. Resolve metadata
        val now = System.currentTimeMillis()
        val staleIds = if (forceRefresh) {
            entriesWithId.map { it.id!! }.toSet()
        } else {
            val cutoff = now - CACHE_DURATION_MS
            val staleFromDb = try { appDao.getStaleAppIds(cutoff).toSet() } catch (e: Exception) { emptySet() }
            val invalidUrlIds = cachedEntities.filter { it.downloadUrl.isBlank() || it.downloadUrl == "#" }.map { it.id }.toSet()
            val cachedIds = cachedEntities.map { it.id }.toSet()
            val missingIds = entriesWithId.map { it.id!! }.filter { it !in cachedIds }.toSet()
            
            // Force resolution if repo metadata changed
            val changedMetadataIds = entriesWithId.filter { newEntry ->
                val oldEntry = cachedEntities.find { it.id == newEntry.id }
                oldEntry != null && (oldEntry.repoUrl != newEntry.repoUrl || oldEntry.releaseKeyword != newEntry.releaseKeyword)
            }.map { it.id!! }.toSet()

            staleFromDb + missingIds + invalidUrlIds + changedMetadataIds
        }

        val entriesToResolve = entriesWithId.filter { it.id in staleIds }
        val resolvedApps = resolveEntries(entriesToResolve)

        if (resolvedApps.isNotEmpty()) {
            try {
                val favoriteIds = appDao.getFavoriteAppIds().toSet()
                val existingApps = appDao.getAllAppIds().toSet()
                val entities = resolvedApps.map { app ->
                    val existing = appDao.getAppById(app.id)
                    val isNew = app.id !in existingApps || existing == null
                    
                    val addedAt = if (isNew) now else (existing?.addedAt ?: now)
                    val updateCount = if (!isNew && existing != null && existing.version != app.version) {
                        existing.updateCount + 1
                    } else {
                        existing?.updateCount ?: 0
                    }

                    AppItemEntity.fromAppItem(
                        app.copy(addedAt = addedAt, updateCount = updateCount),
                        isFavorite = app.id in favoriteIds,
                        lastUpdated = now
                    )
                }
                appDao.insertApps(entities)
            } catch (e: Exception) {
                Log.e("AppRepository", "Failed to save resolved apps", e)
            }
        }

        // 5. Final emit
        val finalApps = try {
            appDao.getAllApps().first().map { it.toAppItem() }
        } catch (e: Exception) {
            resolvedApps
        }
        emit(Resource.Success(finalApps))

    }.catch { e ->
        Log.e("AppRepository", "Failing with error: ${e::class.simpleName}: ${e.message}", e)
        emit(Resource.Error("${e::class.simpleName}: ${e.message}"))
    }.flowOn(Dispatchers.IO)

    private suspend fun fetchAllEntries(): List<AppEntryDto> {
        val result = mutableListOf<AppEntryDto>()
        
        // Fetch default URL (exceptions bubble up)
        Log.d("AppRepository", "Fetching default: $DEFAULT_REPO_URL")
        val defaultList = apiService.getAppEntries(DEFAULT_REPO_URL)
        result.addAll(defaultList)

        // Fetch extra repos (catch individually so one bad extra doesn't break default)
        try {
            val extraUrls = repoDao.getAllRepos()
                .filter { it.isEnabled && it.url != DEFAULT_REPO_URL }
                .map { it.url }
            for (url in extraUrls) {
                try {
                    result.addAll(apiService.getAppEntries(url))
                } catch (e: Exception) {
                    Log.e("AppRepository", "Failed extra repo: $url", e)
                }
            }
        } catch (e: Exception) {
            Log.e("AppRepository", "Failed to query extra repos", e)
        }

        return result.filter { it.id != null }.distinctBy { it.id }
    }

    private suspend fun resolveEntries(entries: List<AppEntryDto>): List<AppItem> = coroutineScope {
        val semaphore = Semaphore(15) // Limit to 15 concurrent requests to avoid server-side blocks
        entries.map { entry ->
            async(Dispatchers.IO) {
                semaphore.withPermit {
                    try {
                        githubProvider.resolve(entry)
                    } catch (e: Exception) {
                        Log.e("AppRepository", "Failed to resolve ${entry.id}", e)
                        null
                    }
                }
            }
        }.awaitAll().filterNotNull()
    }

    private suspend fun ensureDefaultRepo() {
        val allRepos = repoDao.getAllRepos()
        val hasCanonical = allRepos.any { it.url == DEFAULT_REPO_URL }
        if (!hasCanonical) {
            allRepos.filter { it.url.contains("ifyoucanfindme973-netizen/sorwe-store-data") || it.url.contains("Developer-For-Git/MOD-STORE-DATA-") }
                .forEach { repoDao.deleteRepo(it) }
            repoDao.insertRepo(RepoSourceEntity(url = DEFAULT_REPO_URL, name = "Default Sorwe Repo", isEnabled = true))
        }
    }

    fun getAppById(id: String): Flow<AppItem?> = appDao.getAppByIdFlow(id).map { it?.toAppItem() }

    fun getAppWithResolution(id: String): Flow<AppItem?> = flow {
        val cached = try { appDao.getAppById(id) } catch (e: Exception) { null }
        emit(cached?.toAppItem())

        if (cached == null || cached.downloadUrl.isBlank() || cached.downloadUrl == "#") {
            try {
                val currentItem = cached?.toAppItem() ?: return@flow
                // Use fresh data for crucial repo links if available, otherwise fallback to cache
                val entries = try { fetchAllEntries() } catch (e: Exception) { emptyList() }
                val freshEntry = entries.find { it.id == id }

                val actualRepoUrl = freshEntry?.repoUrl ?: currentItem.repoUrl

                val entry = AppEntryDto(
                    id              = currentItem.id,
                    name            = currentItem.name,
                    description     = currentItem.description,
                    icon            = currentItem.icon,
                    version         = currentItem.version,
                    latestVersion   = currentItem.version,
                    repoUrl         = actualRepoUrl,
                    githubRepo      = actualRepoUrl
                                        .removePrefix("https://github.com/")
                                        .trimEnd('/'),
                    releaseKeyword  = freshEntry?.releaseKeyword ?: currentItem.releaseKeyword,
                    downloadUrl     = currentItem.downloadUrl,
                    category        = currentItem.category,
                    platform        = currentItem.platform,
                    size            = currentItem.size,
                    author          = currentItem.developer,
                    screenshots     = currentItem.screenshots,
                    featured        = currentItem.featured,
                    type            = if (currentItem.sourceType == "direct") "direct" else "github"
                )
                val resolved = githubProvider.resolve(entry)
                val isNew = appDao.getAppById(resolved.id) == null
                val addedAt = if (isNew) System.currentTimeMillis() else (cached.addedAt)
                val updateCount = if (!isNew && cached.version != resolved.version) cached.updateCount + 1 else cached.updateCount
                
                val entity = AppItemEntity.fromAppItem(
                    resolved.copy(addedAt = addedAt, updateCount = updateCount),
                    isFavorite = cached.isFavorite, 
                    lastUpdated = System.currentTimeMillis()
                )
                appDao.insertApp(entity)
                emit(resolved)
            } catch (e: Exception) {
                Log.e("AppRepository", "Proactive resolve failed for $id", e)
            }
        }
    }.catch { e ->
        Log.e("AppRepository", "getAppWithResolution error", e)
    }.flowOn(Dispatchers.IO)

    fun getFavoriteApps(): Flow<List<AppItem>> =
        appDao.getFavoriteApps().map { it.map { entity -> entity.toAppItem() } }

    suspend fun toggleFavorite(appId: String) {
        try {
            val current = appDao.isFavorite(appId) ?: false
            appDao.updateFavorite(appId, !current)
        } catch (e: Exception) {
            Log.e("AppRepository", "toggleFavorite failed", e)
        }
    }

    suspend fun isFavorite(appId: String): Boolean =
        try { appDao.isFavorite(appId) ?: false } catch (e: Exception) { false }
}
