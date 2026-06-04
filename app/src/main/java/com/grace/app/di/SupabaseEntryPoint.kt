package com.grace.app.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient

/**
 * Composable-friendly access to the SupabaseClient. Hilt doesn't have a
 * `hiltSingleton<T>()` for Composables, so we use the standard EntryPoint
 * pattern (same shape as the Glance widget's data access).
 *
 * Only the Google native-sign-in composable currently needs this; for
 * everything else, prefer constructor injection into ViewModels/Repos.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface SupabaseEntryPoint {
    fun supabaseClient(): SupabaseClient
}
