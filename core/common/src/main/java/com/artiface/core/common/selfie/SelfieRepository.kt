package com.artiface.core.common.selfie

import com.artiface.core.model.CapturedSelfie
import kotlinx.coroutines.flow.Flow

/**
 * App-scoped registry for captured selfies.
 * Images live in application storage; metadata is keyed by [CapturedSelfie.id].
 */
interface SelfieRepository {
    suspend fun save(selfie: CapturedSelfie)

    suspend fun getById(id: String): CapturedSelfie?

    fun observeById(id: String): Flow<CapturedSelfie?>

    suspend fun delete(id: String)
}
