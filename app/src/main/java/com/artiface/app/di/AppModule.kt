package com.artiface.app.di

import com.artiface.app.BuildConfig
import com.artiface.core.common.di.AppDispatchers
import com.artiface.core.common.di.AppVersion
import com.artiface.core.network.config.NetworkConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDispatchers(): AppDispatchers = AppDispatchers()

    @Provides
    @AppVersion
    @Singleton
    fun provideAppVersion(): String = BuildConfig.VERSION_NAME

    @Provides
    @Singleton
    fun provideNetworkConfig(): NetworkConfig =
        NetworkConfig(
            baseUrl = BuildConfig.ARTIFACE_BASE_URL,
            useRemoteGenerator = BuildConfig.USE_REMOTE_GENERATOR,
            enableHttpLogging = BuildConfig.DEBUG,
        )
}
