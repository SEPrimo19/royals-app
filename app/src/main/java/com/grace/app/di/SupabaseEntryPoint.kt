package com.grace.app.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SupabaseEntryPoint {
    fun supabaseClient(): SupabaseClient
}
