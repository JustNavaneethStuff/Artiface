package com.artiface.core.common.selfie

import com.artiface.core.model.CapturedSelfie
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap

class FakeSelfieRepository(
    initial: Map<String, CapturedSelfie> = emptyMap(),
) : SelfieRepository {

    private val store = ConcurrentHashMap(initial)
    private val version = MutableStateFlow(0L)

    override suspend fun save(selfie: CapturedSelfie) {
        store[selfie.id] = selfie
        version.update { it + 1 }
    }

    override suspend fun getById(id: String): CapturedSelfie? = store[id]

    override fun observeById(id: String): Flow<CapturedSelfie?> =
        version.map { store[id] }

    override suspend fun delete(id: String) {
        store.remove(id)
        version.update { it + 1 }
    }
}
