package com.artiface.core.common.result

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultTest {

    @Test
    fun success_maps_value() {
        val result = Result.Success(2).map { it * 3 }
        assertTrue(result is Result.Success)
        assertEquals(6, result.getOrNull())
    }

    @Test
    fun error_preserves_exception_through_map() {
        val boom = IllegalStateException("boom")
        val error: Result<Int> = Result.Error(boom)
        val result = error.map { it + 1 }
        assertTrue(result.isError)
        assertEquals(boom, result.exceptionOrNull())
    }

    @Test
    fun runCatchingResult_wraps_thrown_exception() {
        val result = runCatchingResult<Int> { error("nope") }
        assertTrue(result.isError)
    }
}
