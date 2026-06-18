package com.grace.app.widget

import com.grace.app.data.local.GraceDatabase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun graceDatabase(): GraceDatabase
}
