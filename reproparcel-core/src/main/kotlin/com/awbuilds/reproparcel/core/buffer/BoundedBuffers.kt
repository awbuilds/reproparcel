// SPDX-License-Identifier: AGPL-3.0-only
package com.awbuilds.reproparcel.core.buffer

import com.awbuilds.reproparcel.core.limits.Limits
import com.awbuilds.reproparcel.core.model.ReportEvent
import com.awbuilds.reproparcel.core.model.StructuredLogEvent
import com.awbuilds.reproparcel.core.time.Clock
import com.awbuilds.reproparcel.core.time.SystemClock

public class BoundedEventBuffer(
    private val maxEvents: Int = Limits.MAX_EVENTS,
    private val maxBytes: Long = Limits.MAX_EVIDENCE_BUFFER_BYTES,
    private val maxAgeMs: Long = Limits.MAX_ACTION_AGE_MS,
    private val clock: Clock = SystemClock,
) {
    private data class Entry(val event: ReportEvent, val addedAtMs: Long, val bytes: Long)

    private val entries = ArrayDeque<Entry>()
    private var bytes: Long = 0

    public fun add(event: ReportEvent): Boolean {
        if (maxEvents <= 0 || maxBytes <= 0 || maxAgeMs <= 0) return false
        val size = eventSize(event)
        if (size > maxBytes) return false
        entries.addLast(Entry(event, clock.nowMs(), size))
        bytes += size
        trim()
        return true
    }

    public fun snapshot(nowMs: Long = clock.nowMs()): List<ReportEvent> {
        pruneByAge(nowMs)
        return entries.map { it.event }
    }

    public fun clear() {
        entries.clear()
        bytes = 0
    }

    public val size: Int get() = entries.size
    public val approximateBytes: Long get() = bytes

    private fun trim() {
        while (entries.size > maxEvents || bytes > maxBytes) {
            val removed = entries.removeFirstOrNull() ?: break
            bytes -= removed.bytes
        }
    }

    private fun pruneByAge(nowMs: Long) {
        while (true) {
            val first = entries.firstOrNull() ?: return
            if (nowMs - first.addedAtMs <= maxAgeMs) return
            entries.removeFirst()
            bytes -= first.bytes
        }
    }

    private fun eventSize(event: ReportEvent): Long {
        var size = 64L
        size += event.category.length
        size += event.actionId?.length ?: 0
        size += event.screenId?.length ?: 0
        size += event.outcome?.length ?: 0
        event.attributes.forEach { (key, value) ->
            size += key.length + value.length + 8
        }
        return size
    }
}

public class BoundedLogBuffer(
    private val maxEvents: Int = Limits.MAX_LOGS,
    private val maxBytes: Long = Limits.MAX_LOG_BYTES,
    private val maxAgeMs: Long = Limits.MAX_ACTION_AGE_MS,
    private val clock: Clock = SystemClock,
) {
    private data class Entry(val event: StructuredLogEvent, val addedAtMs: Long, val bytes: Long)

    private val entries = ArrayDeque<Entry>()
    private var bytes: Long = 0

    public fun add(event: StructuredLogEvent): Boolean {
        if (maxEvents <= 0 || maxBytes <= 0 || maxAgeMs <= 0) return false
        val size = eventSize(event)
        if (size > maxBytes) return false
        entries.addLast(Entry(event, clock.nowMs(), size))
        bytes += size
        trim()
        return true
    }

    public fun snapshot(nowMs: Long = clock.nowMs()): List<StructuredLogEvent> {
        pruneByAge(nowMs)
        return entries.map { it.event }
    }

    public fun clear() {
        entries.clear()
        bytes = 0
    }

    public val size: Int get() = entries.size
    public val approximateBytes: Long get() = bytes

    private fun trim() {
        while (entries.size > maxEvents || bytes > maxBytes) {
            val removed = entries.removeFirstOrNull() ?: break
            bytes -= removed.bytes
        }
    }

    private fun pruneByAge(nowMs: Long) {
        while (true) {
            val first = entries.firstOrNull() ?: return
            if (nowMs - first.addedAtMs <= maxAgeMs) return
            entries.removeFirst()
            bytes -= first.bytes
        }
    }

    private fun eventSize(event: StructuredLogEvent): Long {
        var size = 48L
        size += event.level.length + event.code.length
        event.attributes.forEach { (key, value) ->
            size += key.length + value.length + 8
        }
        return size
    }
}

public class BoundedNetworkBuffer(
    private val maxEntries: Int = Limits.MAX_NETWORK_ENTRIES,
    private val maxBytes: Long = Limits.MAX_NETWORK_BYTES,
) {
    private data class Entry(val metadata: com.awbuilds.reproparcel.core.model.NetworkMetadata, val bytes: Long)

    private val entries = ArrayDeque<Entry>()
    private var bytes: Long = 0

    public fun add(metadata: com.awbuilds.reproparcel.core.model.NetworkMetadata): Boolean {
        val size = 32L + metadata.method.length +
            (metadata.routeTemplate?.length ?: 0) + metadata.outcome.length
        if (size > maxBytes) return false
        entries.addLast(Entry(metadata, size))
        bytes += size
        while (entries.size > maxEntries || bytes > maxBytes) {
            val removed = entries.removeFirstOrNull() ?: break
            bytes -= removed.bytes
        }
        return true
    }

    public fun snapshot(): List<com.awbuilds.reproparcel.core.model.NetworkMetadata> =
        entries.map { it.metadata }

    public fun clear() {
        entries.clear()
        bytes = 0
    }

    public val size: Int get() = entries.size
    public val approximateBytes: Long get() = bytes
}
