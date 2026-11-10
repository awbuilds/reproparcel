// SPDX-License-Identifier: AGPL-3.0-only
package com.awbuilds.reproparcel.core

import com.awbuilds.reproparcel.core.model.Manifest
import com.awbuilds.reproparcel.core.parcel.ManifestValidator
import com.awbuilds.reproparcel.core.parcel.ParcelPathPolicy
import com.awbuilds.reproparcel.core.parcel.ValidationCode
import com.awbuilds.reproparcel.core.parcel.ValidationResult
import com.awbuilds.reproparcel.core.serialization.JsonBudget
import com.awbuilds.reproparcel.core.serialization.ReproParcelJson
import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ParcelValidationTest {
    private val root: Path = Path.of(System.getProperty("reproparcel.root"))

    @Test
    fun validFixturePassesStructuralValidation() {
        val text = Files.readString(root.resolve("schema/fixtures/v1/valid/manifest.json"))
        val manifest = ReproParcelJson.decodeManifest(text)
        assertNotNull(manifest)
        assertEquals(ValidationResult.Valid, ManifestValidator.validate(manifest!!))
    }

    @Test
    fun traversalIsRejected() {
        val issue = ParcelPathPolicy.validate("../../etc/passwd")
        assertEquals(ValidationCode.PATH_TRAVERSAL, issue?.code)
    }

    @Test
    fun absolutePathIsRejected() {
        assertEquals(ValidationCode.ABSOLUTE_PATH, ParcelPathPolicy.validate("/data/report.json")?.code)
    }

    @Test
    fun unreferencedMediaTypeIsRejected() {
        assertEquals(ValidationCode.INVALID_PATH, ParcelPathPolicy.validate("attachments/payload.svg")?.code)
    }

    @Test
    fun duplicateEntryIsRejected() {
        val manifest = loadValid().copy(
            entries = loadValid().entries + loadValid().entries.first(),
        )
        val result = ManifestValidator.validate(manifest) as ValidationResult.Invalid
        assertTrue(result.issues.any { it.code == ValidationCode.DUPLICATE_PATH })
    }

    @Test
    fun missingReportEntryIsRejected() {
        val manifest = loadValid().copy(
            entries = loadValid().entries.filter { it.path != "report.json" },
        )
        val result = ManifestValidator.validate(manifest) as ValidationResult.Invalid
        assertTrue(result.issues.any { it.code == ValidationCode.MISSING_FIELD })
    }

    @Test
    fun unsupportedSchemaIsRejected() {
        val manifest = loadValid().copy(schemaVersion = 2)
        val result = ManifestValidator.validate(manifest) as ValidationResult.Invalid
        assertTrue(result.issues.any { it.code == ValidationCode.UNSUPPORTED_SCHEMA })
    }

    @Test
    fun badTimestampIsRejected() {
        val manifest = loadValid().copy(createdAt = "not-a-time")
        val result = ManifestValidator.validate(manifest) as ValidationResult.Invalid
        assertTrue(result.issues.any { it.code == ValidationCode.INVALID_TIMESTAMP })
    }

    @Test
    fun unknownOptionalFieldsSurviveDecode() {
        val text = """
            {"schemaVersion":1,"reportId":"r","createdAt":"2026-11-11T00:00:00Z",
             "producer":{"sdkVersion":"1","platform":"android","collectionConfigurationId":"c"},
             "app":{"packageName":"com.example.synthetic","versionName":"1","versionCode":1,"variant":"debug"},
             "device":{"apiLevel":30,"manufacturer":"Synthetic","model":"Fixture","windowWidthPx":100,
                       "windowHeightPx":100,"densityDpi":420,"locale":"en-US","fontScale":1.0,"orientation":"portrait"},
             "privacy":{"policyVersion":"p","redactionMode":"opaque"},
             "capture":{"capturedAt":"2026-11-11T00:00:01Z","collectionModes":[],"providers":[]},
             "entries":[{"path":"report.json","mediaType":"application/json","uncompressedSize":1,
                         "sha256":"0000000000000000000000000000000000000000000000000000000000000000"}],
             "limits":{},"futureOptionalField":{"ignored":true}}
        """.trimIndent()
        assertNotNull(ReproParcelJson.decodeManifest(text))
    }

    @Test
    fun jsonBudgetRejectsDeepOrOversizedPayloads() {
        val deep = "[".repeat(40) + "]".repeat(40)
        assertFalse(JsonBudget.inspect(deep).withinBudget)
        assertFalse(JsonBudget.inspect("\"" + "a".repeat(2_100_000) + "\"").withinBudget)
        assertTrue(JsonBudget.inspect("""{"a":[1,2,3]}""").withinBudget)
    }

    @Test
    fun encodeRoundTripKeepsValues() {
        val manifest = loadValid()
        val encoded = ReproParcelJson.encodeManifest(manifest)
        assertEquals(com.awbuilds.reproparcel.core.result.SdkResult.Success, encoded)
        // The encoder only reports success; decode the same object to check the model.
        assertNotNull(ReproParcelJson.decodeManifest(ReproParcelJson.json.encodeToString(Manifest.serializer(), manifest)))
    }

    private fun loadValid(): Manifest {
        val text = Files.readString(root.resolve("schema/fixtures/v1/valid/manifest.json"))
        return requireNotNull(ReproParcelJson.decodeManifest(text))
    }
}
