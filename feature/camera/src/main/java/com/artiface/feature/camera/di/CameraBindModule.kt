package com.artiface.feature.camera.di

import com.artiface.core.common.selfie.SelfieRepository
import com.artiface.feature.camera.data.FileSelfieRepository
import com.artiface.feature.camera.domain.SelfieCaptureStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CameraBindModule {

    @Binds
    @Singleton
    abstract fun bindSelfieRepository(impl: FileSelfieRepository): SelfieRepository

    @Binds
    @Singleton
    abstract fun bindSelfieCaptureStore(impl: FileSelfieRepository): SelfieCaptureStore
}
