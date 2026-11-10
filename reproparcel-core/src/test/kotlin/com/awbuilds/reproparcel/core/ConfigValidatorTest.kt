// SPDX-License-Identifier: AGPL-3.0-only
package com.awbuilds.reproparcel.core

import com.awbuilds.reproparcel.core.config.CapturePolicy
import com.awbuilds.reproparcel.core.config.ConfigErrorCode
import com.awbuilds.reproparcel.core.config.ConfigValidation
import com.awbuilds.reproparcel.core.config.ConfigValidator
import com.awbuilds.reproparcel.core.config.EvidenceConfig
import com.awbuilds.reproparcel.core.config.ReproParcelConfig
import com.awbuilds.reproparcel.core.config.RetentionPolicy
import com.awbuilds.reproparcel.core.model.BuildInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigValidatorTest {
    private val build = BuildInfo(
        packageName = "com.example.synthetic",
        versionName = "1.0.0",
        versionCode = 1,
        variant = "debug",
        gitCommit = "0123456789abcdef",
    )

    @Test
    fun acceptsExplicitDisabledConfig() {
        val config = ReproParcelConfig(enabled = false, buildInfo = build)
        assertEquals(ConfigValidation.Valid, ConfigValidator.validate(config))
        assertEquals(16, ConfigValidator.collectionConfigurationId(config).length)
    }

    @Test
    fun rejectsInvalidPackageAndVersion() {
        val result = ConfigValidator.validate(
            ReproParcelConfig(
                buildInfo = build.copy(packageName = "Not A Package", versionName = "", versionCode = 0),
            ),
        )
        val errors = (result as ConfigValidation.Invalid).errors.map { it.code }
        assertTrue(ConfigErrorCode.INVALID_PACKAGE in errors)
        assertTrue(ConfigErrorCode.INVALID_VERSION_NAME in errors)
        assertTrue(ConfigErrorCode.INVALID_VERSION_CODE in errors)
    }

    @Test
    fun screenshotRequiresPolicy() {
        val result = ConfigValidator.validate(
            ReproParcelConfig(
                buildInfo = build,
                evidence = EvidenceConfig(screenshotEnabled = true),
            ),
        )
        assertTrue(
            (result as ConfigValidation.Invalid).errors.any {
                it.code == ConfigErrorCode.SCREENSHOT_WITHOUT_POLICY
            },
        )
    }

    @Test
    fun rejectsOverlappingSurfaces() {
        val result = ConfigValidator.validate(
            ReproParcelConfig(
                buildInfo = build,
                capturePolicy = CapturePolicy(
                    policyVersion = "v1",
                    screenshotEnabled = true,
                    eligibleSurfaces = setOf("checkout"),
                    sensitiveSurfaces = setOf("checkout"),
                ),
                evidence = EvidenceConfig(screenshotEnabled = true),
            ),
        )
        assertTrue(
            (result as ConfigValidation.Invalid).errors.any {
                it.code == ConfigErrorCode.SURFACE_OVERLAP
            },
        )
    }

    @Test
    fun rejectsInvalidRetention() {
        val result = ConfigValidator.validate(
            ReproParcelConfig(
                buildInfo = build,
                retention = RetentionPolicy(maxReports = 0, maxBytes = 0, ttlHours = 0),
            ),
        )
        assertTrue(
            (result as ConfigValidation.Invalid).errors.any {
                it.code == ConfigErrorCode.INVALID_RETENTION
            },
        )
    }
}
