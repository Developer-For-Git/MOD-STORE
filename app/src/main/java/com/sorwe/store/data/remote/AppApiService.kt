package com.sorwe.store.data.remote

import retrofit2.http.GET
import retrofit2.http.Url

/**
 * Fetches app entries from a raw JSON URL (e.g. GitHub raw content).
 * Cache-busting is handled by OkHttp headers (Cache-Control: no-cache).
 */
interface AppApiService {

    @GET
    suspend fun getAppEntries(
        @Url url: String
    ): List<AppEntryDto>

    @GET
    suspend fun getLatestReleaseNotes(
        @Url url: String
    ): ReleaseNotesDto
}
