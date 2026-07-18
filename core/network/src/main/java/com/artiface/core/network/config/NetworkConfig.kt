package com.artiface.core.network.config

/**
 * Environment knobs for the network layer.
 * Provided from the app [BuildConfig] so debug/release can differ without flavors.
 */
data class NetworkConfig(
    val baseUrl: String,
    val useRemoteGenerator: Boolean,
    val enableHttpLogging: Boolean,
)
