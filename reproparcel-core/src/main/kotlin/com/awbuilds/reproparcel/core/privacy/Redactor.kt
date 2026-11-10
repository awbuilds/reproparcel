// SPDX-License-Identifier: AGPL-3.0-only
package com.awbuilds.reproparcel.core.privacy

import com.awbuilds.reproparcel.core.limits.Limits

public enum class SafeAttribute(public val wireName: String) {
    SCREEN_ID("screen_id"),
    ACTION_ID("action_id"),
    ROUTE_TEMPLATE("route_template"),
    HTTP_METHOD("http_method"),
    STATUS_CODE("status_code"),
    DURATION_MS("duration_ms"),
    RESULT("result"),
    CATEGORY("category"),
    COMPONENT("component"),
    ERROR_CODE("error_code"),
    ORIENTATION("orientation"),
    DENSITY("density"),
    FONT_SCALE("font_scale"),
    API_LEVEL("api_level"),
    MANUFACTURER("manufacturer"),
    MODEL("model"),
    APP_VERSION("app_version"),
    VARIANT("variant"),
    GIT_COMMIT("git_commit"),
    ;

    public companion object {
        private val byName = entries.associateBy { it.wireName }
        public fun fromWireName(name: String): SafeAttribute? = byName[name]
    }
}

public object Redactor {
    private val routeTemplate = Regex("^/[A-Za-z0-9_./{}:-]{0,120}$")
    private val email = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
    private val bearer = Regex("(?i)\\b(bearer|basic)\\s+[A-Za-z0-9._~+/=-]+")
    private val longDigits = Regex("\\d{7,}")
    private val unsafeUrl = Regex("(?i)(https?|ftp)://|\\?|#")
    private val control = Regex("[\\u0000-\\u001F\\u007F]")
    private val pathTraversal = Regex("\\.\\.")
    private val base64Like = Regex("^[A-Za-z0-9+/]{40,}={0,2}$")

    /**
     * Drops unknown keys and unsafe values. This is an allowlist, not a
     * general-purpose detector; callers must still treat report text as
     * untrusted.
     */
    public fun sanitizeAttributes(raw: Map<String, String>): Map<String, String> {
        val result = linkedMapOf<String, String>()
        for ((key, value) in raw) {
            val attribute = SafeAttribute.fromWireName(key) ?: continue
            val safe = sanitize(attribute, value) ?: continue
            result[attribute.wireName] = safe
        }
        return result
    }

    public fun sanitize(attribute: SafeAttribute, value: String): String? {
        val trimmed = value.trim()
        if (trimmed.isEmpty() || trimmed.length > Limits.MAX_ATTRIBUTE_LENGTH) return null
        if (control.containsMatchIn(trimmed)) return null
        if (email.containsMatchIn(trimmed)) return null
        if (bearer.containsMatchIn(trimmed)) return null
        if (pathTraversal.containsMatchIn(trimmed)) return null
        if (base64Like.matches(trimmed)) return null

        return when (attribute) {
            SafeAttribute.ROUTE_TEMPLATE -> {
                if (unsafeUrl.containsMatchIn(trimmed)) null
                else if (!routeTemplate.matches(trimmed)) null
                else trimmed
            }

            SafeAttribute.HTTP_METHOD -> {
                if (trimmed.uppercase() in setOf("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS")) {
                    trimmed.uppercase()
                } else {
                    null
                }
            }

            SafeAttribute.STATUS_CODE,
            SafeAttribute.DURATION_MS,
            SafeAttribute.DENSITY,
            SafeAttribute.API_LEVEL -> {
                if (trimmed.all(Char::isDigit) && trimmed.toLongOrNull() != null) trimmed else null
            }

            SafeAttribute.FONT_SCALE -> {
                val parsed = trimmed.toFloatOrNull() ?: return null
                if (parsed in 0.5f..4.0f) trimmed else null
            }

            SafeAttribute.ORIENTATION -> {
                if (trimmed in setOf("portrait", "landscape", "unknown")) trimmed else null
            }

            SafeAttribute.VARIANT -> {
                if (trimmed.matches(Regex("^[A-Za-z0-9_.-]{1,32}$"))) trimmed else null
            }

            SafeAttribute.GIT_COMMIT -> {
                if (trimmed.matches(Regex("^[0-9a-f]{7,64}$"))) trimmed else null
            }

            SafeAttribute.COMPONENT,
            SafeAttribute.ERROR_CODE,
            SafeAttribute.CATEGORY,
            SafeAttribute.RESULT,
            SafeAttribute.SCREEN_ID,
            SafeAttribute.ACTION_ID,
            SafeAttribute.MANUFACTURER,
            SafeAttribute.MODEL,
            SafeAttribute.APP_VERSION -> {
                if (unsafeUrl.containsMatchIn(trimmed)) return null
                if (longDigits.containsMatchIn(trimmed)) return null
                if (trimmed.length > 120) null else trimmed
            }
        }
    }

    public fun safeDiagnosticClass(simpleClassName: String): String {
        val sanitized = simpleClassName.filter { it.isLetterOrDigit() || it == '_' || it == '$' }
        return sanitized.take(80).ifEmpty { "Unknown" }
    }
}
