// SPDX-License-Identifier: AGPL-3.0-only
package com.awbuilds.reproparcel.android.draft

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.awbuilds.reproparcel.core.config.RetentionPolicy
import com.awbuilds.reproparcel.core.model.Report
import com.awbuilds.reproparcel.core.time.Clock
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DraftStoreTest {
    private lateinit var context: Context
    private lateinit var store: DraftStore
    private val clock = FakeClock()

    private class FakeClock(var now: Long = 1_000_000L) : Clock {
        override fun nowMs(): Long = now
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        store = DraftStore(context, RetentionPolicy(maxReports = 5), clock)
        store.deleteAll()
    }

    @After
    fun tearDown() {
        store.deleteAll()
    }

    @Test
    fun saveLoadRoundTripUsesEncryptedFiles() {
        val report = Report(
            title = "Synthetic secret title",
            description = "Synthetic secret description",
            expectedBehavior = "Expected",
            actualBehavior = "Actual",
        )
        val image = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 1, 2, 3)
        val saved = store.save(report, image) as DraftSaveResult.Saved
        val loaded = store.load(saved.id) as DraftLoadResult.Loaded
        assertEquals(report, loaded.report)
        assertTrue(image.contentEquals(loaded.imagePng))

        val encrypted = java.io.File(context.noBackupFilesDir, "reproparcel/drafts/${saved.id}/report.enc")
            .readBytes()
        assertFalse(encrypted.toString(Charsets.ISO_8859_1).contains("Synthetic secret", ignoreCase = true))
    }

    @Test
    fun tamperedCiphertextFailsClosed() {
        val saved = store.save(Report(description = "synthetic")) as DraftSaveResult.Saved
        val file = java.io.File(context.noBackupFilesDir, "reproparcel/drafts/${saved.id}/report.enc")
        val bytes = file.readBytes()
        bytes[bytes.lastIndex] = (bytes.last().toInt() xor 0x7f).toByte()
        file.writeBytes(bytes)
        val loaded = store.load(saved.id)
        assertTrue(loaded is DraftLoadResult.Failure)
    }

    @Test
    fun deletedKeyFailsClosed() {
        val saved = store.save(Report(description = "synthetic")) as DraftSaveResult.Saved
        store.deleteKeyForTest()
        val loaded = store.load(saved.id)
        assertTrue(loaded is DraftLoadResult.Failure)
    }

    @Test
    fun quotaRemovesOldestDraft() {
        val quotaStore = DraftStore(
            context,
            RetentionPolicy(maxReports = 2, maxBytes = 25L * 1024 * 1024),
            clock,
        )
        quotaStore.deleteAll()
        val first = quotaStore.save(Report(description = "first")) as DraftSaveResult.Saved
        clock.now += 1
        quotaStore.save(Report(description = "second"))
        clock.now += 1
        quotaStore.save(Report(description = "third"))
        val ids = quotaStore.list().map { it.id }
        assertEquals(2, ids.size)
        assertFalse(first.id in ids)
    }

    @Test
    fun expiredDraftIsPruned() {
        val ttlStore = DraftStore(context, RetentionPolicy(ttlHours = 1), clock)
        ttlStore.deleteAll()
        val saved = ttlStore.save(Report(description = "expiring")) as DraftSaveResult.Saved
        clock.now += 2L * 60L * 60L * 1000L
        assertTrue(ttlStore.list().isEmpty())
        assertTrue(ttlStore.load(saved.id) is DraftLoadResult.NotFound)
    }
}
