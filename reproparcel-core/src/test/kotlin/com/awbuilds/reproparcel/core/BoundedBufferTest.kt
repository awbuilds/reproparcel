// SPDX-License-Identifier: Apache-2.0
package com.awbuilds.reproparcel.core

import com.awbuilds.reproparcel.core.buffer.BoundedEventBuffer
import com.awbuilds.reproparcel.core.buffer.BoundedLogBuffer
import com.awbuilds.reproparcel.core.model.ReportEvent
import com.awbuilds.reproparcel.core.model.StructuredLogEvent
import com.awbuilds.reproparcel.core.time.Clock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BoundedBufferTest {
    private class FakeClock(var now: Long = 0L) : Clock {
        override fun nowMs(): Long = now
    }

    @Test
    fun eventBufferDropsOldestWhenCountIsExceeded() {
        val buffer = BoundedEventBuffer(maxEvents = 3, maxBytes = 10_000, maxAgeMs = 10_000)
        repeat(5) { index ->
            assertTrue(buffer.add(ReportEvent(index.toLong(), 0, "tap")))
        }
        assertEquals(3, buffer.size)
        assertEquals(listOf(2L, 3L, 4L), buffer.snapshot().map { it.sequence })
    }

    @Test
    fun eventBufferDropsByAge() {
        val clock = FakeClock()
        val buffer = BoundedEventBuffer(maxEvents = 10, maxBytes = 10_000, maxAgeMs = 100, clock = clock)
        buffer.add(ReportEvent(1, 0, "tap"))
        clock.now = 101
        buffer.add(ReportEvent(2, 0, "tap"))
        assertEquals(listOf(2L), buffer.snapshot().map { it.sequence })
    }

    @Test
    fun eventBufferRejectsEventLargerThanBudget() {
        val buffer = BoundedEventBuffer(maxEvents = 10, maxBytes = 16, maxAgeMs = 10_000)
        assertFalse(buffer.add(ReportEvent(1, 0, "a".repeat(100))))
        assertEquals(0, buffer.size)
    }

    @Test
    fun logBufferHonoursCountAndBytes() {
        val buffer = BoundedLogBuffer(maxEvents = 2, maxBytes = 10_000, maxAgeMs = 10_000)
        repeat(4) { index ->
            buffer.add(StructuredLogEvent(index.toLong(), 0, "info", "e$index"))
        }
        assertEquals(2, buffer.size)
        assertEquals(listOf(2L, 3L), buffer.snapshot().map { it.sequence })
    }
}
