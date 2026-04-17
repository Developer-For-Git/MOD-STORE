package com.sorwe.store.data.remote

import com.squareup.moshi.Json
import retrofit2.http.*

interface GitHubAuthService {

    @Headers("Accept: application/json")
    @POST("https://github.com/login/oauth/access_token")
    @FormUrlEncoded
    suspend fun getAccessToken(
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("code") code: String,
        @Field("redirect_uri") redirectUri: String
    ): retrofit2.Response<okhttp3.ResponseBody>

    @GET("https://api.github.com/user")
    suspend fun getUserProfile(
        @Header("Authorization") token: String
    ): retrofit2.Response<okhttp3.ResponseBody>
}

data class GitHubTokenResponse(
    @Json(name = "access_token") val accessToken: String? = null,
    @Json(name = "token_type") val tokenType: String? = null,
    @Json(name = "scope") val scope: String? = null,
    @Json(name = "error") val error: String? = null,
    @Json(name = "error_description") val errorDescription: String? = null
)

data class GitHubUserResponse(
    @Json(name = "login") val login: String,
    @Json(name = "avatar_url") val avatarUrl: String,
    @Json(name = "name") val name: String?
)
