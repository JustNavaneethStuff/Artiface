package com.artiface.feature.processing.data

import com.artiface.core.common.generation.LocationContextProvider
import com.artiface.core.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MVP location context: broad label only after explicit preference consent.
 * No GPS is queried yet — that arrives with a real provider later.
 */
@Singleton
class PreferenceAwareLocationContextProvider @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
) : LocationContextProvider {

    override suspend fun broadLocationLabel(): String? {
        val prefs = preferencesRepository.preferences.first()
        if (!prefs.contextualPersonalizationEnabled || !prefs.locationContextEnabled) {
            return null
        }
        return "a nearby neighborhood of mild intrigue"
    }
}
