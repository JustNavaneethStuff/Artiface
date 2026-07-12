package com.artiface.core.testing

import com.artiface.core.common.di.AppDispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher

/**
 * Shared test helpers for deterministic coroutine execution.
 */
@OptIn(ExperimentalCoroutinesApi::class)
object TestDispatchers {
    fun create(dispatcher: TestDispatcher = StandardTestDispatcher()): AppDispatchers =
        AppDispatchers(
            main = dispatcher,
            io = dispatcher,
            default = dispatcher,
        )
}
