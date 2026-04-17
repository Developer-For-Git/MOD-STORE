package com.sorwe.store.data.remote

import android.util.Log
import com.sorwe.store.data.model.AppItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

class GitHubRateLimitException(message: String) : Exception(message)

// ─── Data classes (formerly in GitHubApiService.kt) ──────────────────────────

/** Raw release response from api.github.com */
data class GitHubReleaseResponse(
    val tagName: String = "",
    val name: String = "",
    val assets: List<GitHubReleaseAsset> = emptyList()
)

/** A single asset entry within a GitHub release */
data class GitHubReleaseAsset(
    val name: String = "",
    val browserDownloadUrl: String = "",
    val size: Long = 0L,
    val contentType: String = ""
)

/** One APK variant shown in the architecture-picker dialog */
data class ApkAssetVariant(
    val label: String,       // "Universal", "ARM64", "ARMv7", "x64", "x86"
    val fileName: String,    // original asset filename
    val downloadUrl: String, // direct download URL from GitHub releases
    val sizeBytes: Long,     // file size in bytes
    val tagName: String      // release tag, e.g. "v1.0.2"
)

// ─── Main provider ────────────────────────────────────────────────────────────

/**
 * Single source of truth for resolving GitHub release metadata and download URLs.
 *
 * All requests go directly to api.github.com via OkHttp.
 * No GitHub token is injected, so API requests are subject to the 60 req/hr unauthenticated rate limit.
 *
 * Two in-session caches prevent duplicate API calls within one app session:
 * - [releaseCache]   → latest release per repo
 * - [downloadUrlCache] → resolved download URL per (repo + keyword) pair
 */
@Singleton
class GitHubMetadataProvider @Inject constructor(
    private val okHttpClient: OkHttpClient
) {

    private val releaseCache     = ConcurrentHashMap<String, GitHubReleaseResponse>()
    private val downloadUrlCache = ConcurrentHashMap<String, String>()

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Resolves an [AppEntryDto] to a full [AppItem], fetching the latest GitHub release
     * when needed to obtain a real download URL.
     */
    suspend fun resolve(entry: AppEntryDto): AppItem {
        return try {
            when (entry.type) {
                "direct" -> resolveDirect(entry)
                else     -> resolveGitHub(entry)
            }
        } catch (e: Exception) {
            Log.e(TAG, "resolve failed for ${entry.id}", e)
            buildFallbackItem(entry)
        }
    }

    /**
     * Fetches ALL APK assets from the latest release for [entry] and returns them ordered
     * by architecture preference (Universal first). Used by the architecture-picker dialog.
     */
    suspend fun fetchAllApkVariants(entry: AppEntryDto): List<ApkAssetVariant> {
        val repoString = resolveRepoString(entry)
        val keyword    = entry.releaseKeyword
        Log.d(TAG, "fetchAllApkVariants: repo=$repoString entry.id=${entry.id} kw=$keyword")

        if (repoString == null) {
            Log.e(TAG, "fetchAllApkVariants: could not determine repo for entry ${entry.id}")
            return emptyList()
        }

        val release = findReleaseWithApks(repoString, keyword)
        if (release == null) {
            Log.e(TAG, "fetchAllApkVariants: no release with matching APKs found for $repoString (kw=$keyword)")
            return emptyList()
        }

        Log.d(TAG, "fetchAllApkVariants: tag=${release.tagName} totalAssets=${release.assets.size}")

        val lower = keyword?.lowercase() ?: ""
        val apks = release.assets
            .filter { asset -> 
                asset.name.lowercase().endsWith(".apk") && 
                (lower.isBlank() || asset.name.lowercase().contains(lower))
            }
            .map { asset ->
                ApkAssetVariant(
                    label       = detectArchLabel(asset.name),
                    fileName    = asset.name,
                    downloadUrl = asset.browserDownloadUrl,
                    sizeBytes   = asset.size,
                    tagName     = release.tagName
                )
            }
            .sortedWith(compareBy { archSortOrder(it.label) })

        Log.d(TAG, "fetchAllApkVariants: resolved ${apks.size} APK variants for $repoString (matching kw=$keyword)")
        return apks
    }

    // ─── Private resolution helpers ──────────────────────────────────────────

    private suspend fun resolveGitHub(entry: AppEntryDto): AppItem {
        val repoString   = resolveRepoString(entry)
        val repoUrlFinal = if (repoString != null) "https://github.com/$repoString"
                           else entry.repoUrl ?: ""

        val finalDownloadUrl: String = when {
            // 1. Entry already has a real direct URL — use it
            !entry.downloadUrl.isNullOrBlank() && entry.downloadUrl != "#" -> {
                Log.d(TAG, "resolveGitHub[${entry.id}]: using entry.downloadUrl directly")
                entry.downloadUrl
            }
            // 2. We defer GitHub API fetching until the user actually clicks download 
            //    (which invokes fetchAllApkVariants). This prevents exhausting the
            //    60 req/hr unauthenticated rate limit during full app scans.
            else -> {
                Log.d(TAG, "resolveGitHub[${entry.id}]: deferring GitHub API fetch to prevent rate limits")
                ""
            }
        }

        return AppItem(
            id             = entry.id ?: "unknown",
            name           = entry.name ?: entry.customName ?: entry.id ?: "Unknown",
            description    = entry.description ?: entry.customDescription ?: "No description available",
            version        = entry.latestVersion ?: entry.version ?: "Latest",
            size           = entry.size ?: "Varies",
            category       = entry.category ?: "Apps",
            icon           = entry.icon ?: "",
            screenshots    = entry.screenshots ?: emptyList(),
            downloadUrl    = finalDownloadUrl,
            changelog      = "Latest release from GitHub.",
            developer      = entry.author ?: "GitHub",
            rating         = 0.0,
            featured       = entry.featured,
            banner         = "",
            sourceType     = if (repoString != null) "github" else "mixed",
            platform       = entry.platform ?: "Android",
            repoUrl        = repoUrlFinal,
            releaseKeyword = entry.releaseKeyword ?: ""
        )
    }

    /**
     * Fetches the best-matching APK URL from the latest release.
     * Matching priority: keyword + .apk → keyword alone → any .apk.
     * Returns empty string (not a fake heuristic URL) if nothing matches.
     */
    private suspend fun fetchBestAssetUrl(repoString: String, keyword: String): String {
        val cacheKey = "$repoString|$keyword"
        downloadUrlCache[cacheKey]?.let { return it }

        val release = findReleaseWithApks(repoString, keyword) ?: return ""

        Log.d(TAG, "fetchBestAssetUrl: tag=${release.tagName} assets=${release.assets.size} kw=$keyword")

        val lower = keyword.lowercase()
        val best  = release.assets.find { it.name.lowercase().contains(lower) && it.name.lowercase().endsWith(".apk") }
            ?: release.assets.find { it.name.lowercase().contains(lower) }
            ?: release.assets.find { it.name.lowercase().endsWith(".apk") }

        if (best == null) {
            Log.w(TAG, "fetchBestAssetUrl: no matching asset for kw='$keyword' in $repoString")
            return ""
        }

        Log.d(TAG, "fetchBestAssetUrl: resolved → ${best.browserDownloadUrl}")
        downloadUrlCache[cacheKey] = best.browserDownloadUrl
        return best.browserDownloadUrl
    }

    /** Returns the browser download URL for the first .apk in the best release found, or "". */
    private suspend fun fetchFirstApkUrl(repoString: String): String {
        val release = findReleaseWithApks(repoString, null) ?: return ""
        val apk     = release.assets.firstOrNull { it.name.lowercase().endsWith(".apk") }
        return apk?.browserDownloadUrl ?: ""
    }

    /**
     * Finds the most-recent GitHub release for [repoString] that has at least one .apk asset.
     *
     * Strategy:
     * 1. Try `/releases/latest` — works when the latest release includes APK files.
     * 2. If no APK assets found, scan `/releases?per_page=10` in chronological order
     *    (newest-first) and return the first release that has an APK.
     *
     * This handles repos like shosetsu where newer tags dropped APK uploads.
     */
    private suspend fun findReleaseWithApks(repoString: String, keyword: String?): GitHubReleaseResponse? {
        val lower = keyword?.lowercase()
        
        // 1. Try latest first
        val latest = fetchLatestRelease(repoString)
        if (latest != null) {
            val hasMatchingApk = latest.assets.any { asset ->
                asset.name.lowercase().endsWith(".apk") && 
                (lower == null || asset.name.lowercase().contains(lower))
            }
            if (hasMatchingApk) return latest
        }

        Log.d(TAG, "findReleaseWithApks: latest release for $repoString has no matching APKs (kw=$keyword), scanning recent releases...")

        // 2. Scan recent releases list for one that has matching APK assets
        return fetchReleasesWithApk(repoString, keyword)
    }

    /**
     * Fetches the list of recent releases and returns the first one (newest) that has a matching APK.
     * Scans up to 3 pages (90 releases) to handle complex projects.
     */
    private suspend fun fetchReleasesWithApk(repoString: String, keyword: String?): GitHubReleaseResponse? =
        withContext(Dispatchers.IO) {
            val lower = keyword?.lowercase()
            
            for (page in 1..3) {
                val url = "https://api.github.com/repos/$repoString/releases?per_page=30&page=$page"
                Log.d(TAG, "fetchReleasesWithApk: GET $url")

                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "SorweStore/1.0")
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .build()

                try {
                    val response = okHttpClient.newCall(request).execute()
                    val body     = response.body?.string()
                    if (!response.isSuccessful || body == null) {
                        if (response.code == 403 && body?.contains("rate limit", ignoreCase = true) == true) {
                            throw GitHubRateLimitException("GitHub API rate limit exceeded (60 req/hr unauthenticated). Please try again later.")
                        }
                        Log.e(TAG, "fetchReleasesWithApk FAILED [page $page]: HTTP ${response.code}")
                        break // Stop on error
                    }
                    val arr = org.json.JSONArray(body)
                    if (arr.length() == 0) break // No more releases
                    
                    for (i in 0 until arr.length()) {
                        val jsonObj = arr.getJSONObject(i)
                        val parsed = parseReleaseJson(jsonObj.toString())
                        val hasMatchingApk = parsed.assets.any { asset ->
                            asset.name.lowercase().endsWith(".apk") && 
                            (lower == null || asset.name.lowercase().contains(lower))
                        }
                        if (hasMatchingApk) {
                            Log.d(TAG, "fetchReleasesWithApk: found matching APK in ${parsed.tagName} for $repoString (page $page)")
                            return@withContext parsed
                        }
                    }
                } catch (e: Exception) {
                    if (e is GitHubRateLimitException) throw e
                    Log.e(TAG, "fetchReleasesWithApk EXCEPTION [page $page] for $repoString: ${e.message}")
                    break
                }
            }
            Log.w(TAG, "fetchReleasesWithApk: no matching release found in last 90 releases for $repoString (kw=$keyword)")
            null
        }

    // ─── GitHub API call ─────────────────────────────────────────────────────

    /**
     * Fetches the latest release JSON for [repoString] (e.g. "owner/repo") using
     * OkHttp with the GitHub token in the Authorization header.
     *
     * Results are cached in [releaseCache] for the lifetime of the app session.
     */
    private suspend fun fetchLatestRelease(repoString: String): GitHubReleaseResponse? =
        withContext(Dispatchers.IO) {
            releaseCache[repoString]?.let { return@withContext it }

            val url = "https://api.github.com/repos/$repoString/releases/latest"

            Log.d(TAG, "fetchLatestRelease: GET $url")

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "SorweStore/1.0")
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                // No token is injected, requests are subject to the unauthenticated rate limit.
                .build()

            return@withContext try {
                val response = okHttpClient.newCall(request).execute()
                val body     = response.body?.string()
                Log.d(TAG, "fetchLatestRelease: HTTP ${response.code} bodyLen=${body?.length ?: 0}")

                if (!response.isSuccessful || body == null) {
                    if (response.code == 403 && body?.contains("rate limit", ignoreCase = true) == true) {
                        throw GitHubRateLimitException("GitHub API rate limit exceeded (60 req/hr unauthenticated). Please try again later.")
                    }
                    Log.e(TAG, "fetchLatestRelease FAILED: HTTP ${response.code} body=${body?.take(300)}")
                    null
                } else {
                    val parsed = parseReleaseJson(body)
                    Log.d(TAG, "fetchLatestRelease: parsed tag=${parsed.tagName} assets=${parsed.assets.size}")
                    releaseCache[repoString] = parsed
                    parsed
                }
            } catch (e: Exception) {
                if (e is GitHubRateLimitException) throw e
                Log.e(TAG, "fetchLatestRelease EXCEPTION for $repoString: ${e.javaClass.simpleName}: ${e.message}", e)
                null
            }
        }

    // ─── JSON parsing ────────────────────────────────────────────────────────

    private fun parseReleaseJson(json: String): GitHubReleaseResponse {
        val obj       = JSONObject(json)
        val tagName   = obj.optString("tag_name", "")
        val name      = obj.optString("name", "")
        val assetsArr = obj.optJSONArray("assets")

        val assets = buildList {
            if (assetsArr != null) {
                for (i in 0 until assetsArr.length()) {
                    val a = assetsArr.getJSONObject(i)
                    add(
                        GitHubReleaseAsset(
                            name               = a.optString("name", ""),
                            browserDownloadUrl = a.optString("browser_download_url", ""),
                            size               = a.optLong("size", 0L),
                            contentType        = a.optString("content_type", "")
                        )
                    )
                }
            }
        }
        return GitHubReleaseResponse(tagName = tagName, name = name, assets = assets)
    }

    // ─── Fallback / direct items ─────────────────────────────────────────────

    private fun resolveDirect(entry: AppEntryDto): AppItem = AppItem(
        id             = entry.id ?: "unknown",
        name           = entry.name ?: entry.customName ?: entry.id ?: "Unknown",
        description    = entry.description ?: entry.customDescription ?: "No description available",
        version        = entry.latestVersion ?: entry.version ?: "1.0",
        size           = entry.size ?: "Unknown",
        category       = entry.category ?: "Apps",
        icon           = entry.icon ?: "",
        screenshots    = entry.screenshots ?: emptyList(),
        downloadUrl    = entry.downloadUrl ?: "",
        changelog      = "Direct download link.",
        developer      = entry.author ?: "Direct",
        rating         = 0.0,
        featured       = entry.featured,
        banner         = "",
        sourceType     = "direct",
        platform       = entry.platform ?: "Android",
        repoUrl        = entry.repoUrl ?: entry.githubRepo?.let { "https://github.com/$it" } ?: "",
        releaseKeyword = entry.releaseKeyword ?: ""
    )

    private fun buildFallbackItem(entry: AppEntryDto): AppItem = AppItem(
        id             = entry.id ?: "unknown",
        name           = entry.name ?: entry.customName ?: entry.id ?: "Unknown",
        description    = entry.description ?: "No description available",
        version        = entry.version ?: "Unknown",
        size           = entry.size ?: "Unknown",
        category       = entry.category ?: "Apps",
        icon           = entry.icon ?: "",
        screenshots    = entry.screenshots ?: emptyList(),
        downloadUrl    = entry.downloadUrl ?: "",
        changelog      = "",
        developer      = entry.author ?: "Unknown",
        rating         = 0.0,
        featured       = entry.featured,
        banner         = "",
        sourceType     = "fallback",
        platform       = entry.platform ?: "Android",
        repoUrl        = entry.repoUrl
            ?: entry.githubRepo?.let { raw ->
                val normalized = raw
                    .removePrefix("https://github.com/")
                    .removePrefix("http://github.com/")
                    .trimEnd('/')
                if (normalized.contains("/")) "https://github.com/$normalized" else ""
            } ?: "",
        releaseKeyword = entry.releaseKeyword ?: ""
    )

    // ─── Repo string helpers ─────────────────────────────────────────────────

    /**
     * Extracts a canonical "owner/repo" string from the entry, trying
     * [AppEntryDto.githubRepo] first, then [AppEntryDto.repoUrl].
     */
    private fun resolveRepoString(entry: AppEntryDto): String? =
        normalizeGitHubRepo(entry.githubRepo) ?: extractRepoFromUrl(entry.repoUrl ?: "")

    private fun normalizeGitHubRepo(githubRepo: String?): String? {
        if (githubRepo.isNullOrBlank() || githubRepo == "#") return null
        val cleaned = githubRepo
            .removePrefix("https://github.com/")
            .removePrefix("http://github.com/")
            .removePrefix("https://www.github.com/")
            .removePrefix("http://www.github.com/")
            .trimEnd('/')
        if (cleaned.contains("://")) return null
        val parts = cleaned.split("/")
        return if (parts.size >= 2) "${parts[0]}/${parts[1]}" else null
    }

    private fun extractRepoFromUrl(url: String): String? {
        val clean = url
            .removePrefix("https://github.com/")
            .removePrefix("http://github.com/")
            .removePrefix("https://www.github.com/")
            .removePrefix("http://www.github.com/")
        if (clean == url) return null // no GitHub prefix was removed → not a GitHub URL
        val parts = clean.trimEnd('/').split("/")
        return if (parts.size >= 2) "${parts[0]}/${parts[1]}" else null
    }

    // ─── Architecture detection ──────────────────────────────────────────────

    private fun detectArchLabel(name: String): String {
        val lower = name.lowercase()
        return when {
            lower.contains("universal")                                                -> "Universal"
            lower.contains("arm64-v8a") || lower.contains("arm64_v8a")
                    || lower.contains("arm64")                                         -> "ARM64"
            lower.contains("armeabi-v7a") || lower.contains("armv7")
                    || lower.contains("arm32")                                         -> "ARMv7"
            lower.contains("x86_64") || lower.contains("x64")                         -> "x64"
            lower.contains("x86")                                                      -> "x86"
            else -> name.removeSuffix(".apk").take(24)
        }
    }

    private fun archSortOrder(label: String): Int = when (label) {
        "Universal" -> 0
        "ARM64"     -> 1
        "ARMv7"     -> 2
        "x64"       -> 3
        "x86"       -> 4
        else        -> 5
    }

    companion object {
        private const val TAG = "GitHubMetadataProvider"
    }
}
