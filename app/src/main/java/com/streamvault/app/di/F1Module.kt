package com.streamvault.app.di

import android.content.Context
import android.content.SharedPreferences
import com.streamvault.data.di.F1CalendarPrefs
import com.streamvault.data.remote.f1.JolpicaApiService
import com.streamvault.data.remote.f1.OkHttpJolpicaApiService
import com.streamvault.data.repository.F1CalendarRepositoryImpl
import com.streamvault.domain.repository.F1CalendarRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object F1Module {

    @Provides
    @Singleton
    fun provideJolpicaApiService(okHttpClient: OkHttpClient): JolpicaApiService =
        OkHttpJolpicaApiService(okHttpClient)

    @Provides
    @Singleton
    @F1CalendarPrefs
    fun provideF1CalendarSharedPreferences(
        @ApplicationContext context: Context
    ): SharedPreferences =
        context.getSharedPreferences("f1_calendar_prefs", Context.MODE_PRIVATE)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class F1RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindF1CalendarRepository(
        impl: F1CalendarRepositoryImpl
    ): F1CalendarRepository
}
