package com.artiface.feature.processing.di

import com.artiface.core.common.generation.CaricatureGenerator
import com.artiface.core.common.generation.ExpressionAnalyzer
import com.artiface.core.common.generation.GenerationRepository
import com.artiface.core.common.generation.LocalTimeContextProvider
import com.artiface.core.common.generation.LocationContextProvider
import com.artiface.core.common.generation.TimeContextProvider
import com.artiface.core.network.config.NetworkConfig
import com.artiface.core.network.remote.RemoteCaricatureGenerator
import com.artiface.feature.processing.data.FakeCaricatureGenerator
import com.artiface.feature.processing.data.FakeGenerationRepository
import com.artiface.feature.processing.data.HeuristicExpressionAnalyzer
import com.artiface.feature.processing.data.PreferenceAwareLocationContextProvider
import com.artiface.feature.processing.work.GenerationWorkScheduler
import com.artiface.feature.processing.work.WorkManagerGenerationScheduler
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ProcessingBindModule {

    @Binds
    @Singleton
    abstract fun bindGenerationRepository(impl: FakeGenerationRepository): GenerationRepository

    @Binds
    @Singleton
    abstract fun bindGenerationWorkScheduler(
        impl: WorkManagerGenerationScheduler,
    ): GenerationWorkScheduler

    @Binds
    @Singleton
    abstract fun bindExpressionAnalyzer(impl: HeuristicExpressionAnalyzer): ExpressionAnalyzer

    @Binds
    @Singleton
    abstract fun bindLocationContextProvider(
        impl: PreferenceAwareLocationContextProvider,
    ): LocationContextProvider
}

@Module
@InstallIn(SingletonComponent::class)
object ProcessingProvideModule {

    @Provides
    @Singleton
    fun provideTimeContextProvider(): TimeContextProvider = LocalTimeContextProvider()

    /**
     * Environment switch: fake local generator by default; remote Retrofit path when enabled.
     */
    @Provides
    @Singleton
    fun provideCaricatureGenerator(
        config: NetworkConfig,
        fake: FakeCaricatureGenerator,
        remote: RemoteCaricatureGenerator,
    ): CaricatureGenerator =
        if (config.useRemoteGenerator) remote else fake
}
