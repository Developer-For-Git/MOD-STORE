package com.sorwe.store.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import androidx.annotation.Keep
import com.squareup.moshi.JsonClass
import okhttp3.ResponseBody

@Keep
@JsonClass(generateAdapter = true)
data class IssueRequest(
    val title: String,
    val body: String,
    val labels: List<String> = listOf("app-request")
)

interface GitHubIssueService {
    @POST("repos/{owner}/{repo}/issues")
    suspend fun createIssue(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Header("Authorization") authHeader: String,
        @Body request: IssueRequest
    ): Response<ResponseBody>
}
