// SPDX-License-Identifier: AGPL-3.0-only
package com.awbuilds.reproparcel.core

import com.networknt.schema.InputFormat
import com.networknt.schema.SchemaRegistry
import com.networknt.schema.dialect.Dialects
import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertTrue
import org.junit.Test

class SchemaConformanceTest {
    private val root: Path = Path.of(System.getProperty("reproparcel.root"))
    private val registry = SchemaRegistry.withDialect(Dialects.getDraft202012())

    @Test
    fun validFixturesConformToV1Schemas() {
        assertValid("manifest.schema.json", "valid/manifest.json")
        assertValid("report.schema.json", "valid/report.json")
        assertValid("events.schema.json", "valid/events.json")
        assertValid("semantics.schema.json", "valid/semantics.json")
    }

    @Test
    fun invalidFixturesAreRejected() {
        assertInvalid("manifest.schema.json", "invalid/manifest-bad-timestamp.json")
        assertInvalid("manifest.schema.json", "invalid/manifest-unsupported-schema.json")
        assertInvalid("report.schema.json", "invalid/report-missing-description.json")
        assertInvalid("events.schema.json", "invalid/events-negative-offset.json")
    }

    @Test
    fun semanticallyTooManyNodesIsRejected() {
        val nodes = (1..501).joinToString(",") {
            """{"nodeId":"n$it","role":"text","bounds":{"left":0,"top":0,"right":10,"bottom":10}}"""
        }
        val json = """{"nodes":[$nodes]}"""
        assertInvalidJson("semantics.schema.json", json)
    }

    private fun assertValid(schemaName: String, fixture: String) {
        val errors = validate(schemaName, fixtureText(fixture))
        assertTrue("$fixture should validate: $errors", errors.isEmpty())
    }

    private fun assertInvalid(schemaName: String, fixture: String) {
        val errors = validate(schemaName, fixtureText(fixture))
        assertTrue("$fixture should be rejected", errors.isNotEmpty())
    }

    private fun assertInvalidJson(schemaName: String, json: String) {
        val errors = validate(schemaName, json)
        assertTrue("generated payload should be rejected: $errors", errors.isNotEmpty())
    }

    private fun validate(schemaName: String, json: String): List<Any> {
        val schemaText = Files.readString(root.resolve("schema/v1/$schemaName"))
        val schema = registry.getSchema(schemaText, InputFormat.JSON)
        return schema.validate(json, InputFormat.JSON) { context ->
            context.executionConfig { it.formatAssertionsEnabled(true) }
        }
    }

    private fun fixtureText(fixture: String): String =
        Files.readString(root.resolve("schema/fixtures/v1/$fixture"))
}
