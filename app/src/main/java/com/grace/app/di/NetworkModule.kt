package com.grace.app.di

import com.grace.app.BuildConfig
import com.grace.app.data.remote.bible.BibleApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.compose.auth.ComposeAuth
import io.github.jan.supabase.compose.auth.googleNativeLogin
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ) {
        install(Auth) {
            // OAuth callback target (kept as a fallback for the browser flow).
            // Supabase builds the redirect URL as "$scheme://$host" — must be
            // whitelisted in Supabase Dashboard → Auth → URL Configuration →
            // Redirect URLs AND match the intent-filter in AndroidManifest.
            scheme = "grace"
            host = "login-callback"
        }
        install(Postgrest)
        install(Realtime)
        install(Storage)
        install(Functions)
        // Native Google One Tap. `serverClientId` is the Google **Web** OAuth
        // client ID (the same one entered in Supabase's Google provider).
        // When blank, the button will surface a clear "Google sign-in isn't
        // configured" error instead of crashing.
        install(ComposeAuth) {
            googleNativeLogin(serverClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID)
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
        // bible-api.com is public; no Authorization header needed. When/if you
        // swap in a licensed NKJV provider, conditionally add the auth header
        // here based on BuildConfig.BIBLE_API_KEY being non-empty.
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
        }
        return builder.build()
    }

    @Provides
    @Singleton
    fun provideBibleApiService(okHttpClient: OkHttpClient): BibleApiService =
        Retrofit.Builder()
            .baseUrl("https://bible-api.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BibleApiService::class.java)
}
