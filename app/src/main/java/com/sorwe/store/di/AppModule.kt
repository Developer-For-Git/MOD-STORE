package com.sorwe.store.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import android.util.Log
import com.sorwe.store.data.local.AppDao
import com.sorwe.store.data.local.AppDatabase
import com.sorwe.store.data.preferences.UserPreferences
import com.sorwe.store.data.remote.AppApiService
import com.sorwe.store.data.remote.GitHubIssueService
import com.sorwe.store.data.remote.GitHubAuthService
import com.sorwe.store.data.repository.AppRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.sorwe.store.util.AppBackupManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun provideOkHttpClient(userPreferences: UserPreferences): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                val builder = request.newBuilder()
                    .header("User-Agent", "SorweStore/1.0")
                
                // Add GitHub Token if available and request is to GitHub API
                if (request.url.host == "api.github.com") {
                    runBlocking {
                        userPreferences.githubAccessToken.firstOrNull()?.let { token ->
                            builder.header("Authorization", "Bearer $token")
                        }
                    }
                }
                
                chain.proceed(builder.build())
            }
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    // ─── Retrofit instances ─────────────────────────────────────────

    @Provides
    @Singleton
    @Named("github_raw")
    fun provideGitHubRawRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://raw.githubusercontent.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    @Provides
    @Singleton
    fun provideAppApiService(@Named("github_raw") retrofit: Retrofit): AppApiService =
        retrofit.create(AppApiService::class.java)

    @Provides
    @Singleton
    @Named("github_api")
    fun provideGitHubApiRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    @Provides
    @Singleton
    fun provideGitHubIssueService(@Named("github_api") retrofit: Retrofit): GitHubIssueService =
        retrofit.create(GitHubIssueService::class.java)

    @Provides
    @Singleton
    fun provideGitHubAuthService(okHttpClient: OkHttpClient, moshi: Moshi): GitHubAuthService {
        return Retrofit.Builder()
            .baseUrl("https://github.com/")
            .client(okHttpClient)
            .addConverterFactory(retrofit2.converter.scalars.ScalarsConverterFactory.create())
            .addConverterFactory(retrofit2.converter.moshi.MoshiConverterFactory.create(moshi))
            .build()
            .create(GitHubAuthService::class.java)
    }

    // ─── Database ───────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "sorwe_store.db"
        )
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6,
                AppDatabase.MIGRATION_6_7,
                AppDatabase.MIGRATION_7_8,
                AppDatabase.MIGRATION_8_9,
                AppDatabase.MIGRATION_9_10
            )
            .fallbackToDestructiveMigration()
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    super.onCreate(db)
                    db.execSQL(
                        "INSERT INTO `repos` (`url`, `name`, `isEnabled`) VALUES ('${AppRepository.DEFAULT_REPO_URL}', 'Default Sorwe Repo', 1)"
                    )
                }
            })
            .build()

    @Provides
    @Singleton
    fun provideAppDao(database: AppDatabase): AppDao =
        database.appDao()

    @Provides
    @Singleton
    fun provideRepoDao(database: AppDatabase): com.sorwe.store.data.local.RepoDao =
        database.repoDao()

    // ─── Preferences ────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideUserPreferences(@ApplicationContext context: Context): UserPreferences =
        UserPreferences(context)
    // ─── Utilities ────────────────────────────────────────────────
    
    @Provides
    @Singleton
    fun provideAppBackupManager(@ApplicationContext context: Context): AppBackupManager =
        AppBackupManager(context)
}
