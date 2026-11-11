// SPDX-License-Identifier: AGPL-3.0-only
package com.awbuilds.reproparcel.okhttp

import com.awbuilds.reproparcel.core.model.NetworkMetadata
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MetadataInterceptorTest {
    private val server = MockWebServer()

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun recordsOnlySafeMetadataAndPreservesResponse() {
        server.enqueue(MockResponse().setResponseCode(201).setBody("synthetic"))
        val captured = mutableListOf<NetworkMetadata>()
        val client = OkHttpClient.Builder()
            .addInterceptor(MetadataInterceptor(sink = { captured += it }))
            .build()
        val request = Request.Builder()
            .url(server.url("/orders/123?token=secret"))
            .tag(RouteTemplate::class.java, RouteTemplate("/orders/{id}"))
            .build()
        val response = client.newCall(request).execute()
        assertEquals(201, response.code)
        assertEquals("synthetic", response.body?.string())
        val metadata = captured.single()
        assertEquals("GET", metadata.method)
        assertEquals(201, metadata.statusCode)
        assertEquals("/orders/{id}", metadata.routeTemplate)
        assertTrue(metadata.durationMs >= 0)
        assertFalse(metadata.routeTemplate?.contains("secret") == true)
    }

    @Test
    fun disconnectedNetworkExceptionIsPreservedUnchanged() {
        val client = OkHttpClient.Builder()
            .addInterceptor(MetadataInterceptor(sink = { }))
            .build()
        val request = Request.Builder().url("http://127.0.0.1:1/unreachable").build()
        val failure = try {
            client.newCall(request).execute()
            null
        } catch (error: java.io.IOException) {
            error
        }
        assertTrue("expected the original network exception", failure is java.io.IOException)
    }

    @Test
    fun sinkFailureDoesNotChangeResponse() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("still-here"))
        val client = OkHttpClient.Builder()
            .addInterceptor(
                MetadataInterceptor(
                    sink = { error("synthetic sink failure") },
                    onSinkFailure = { },
                ),
            )
            .build()
        val response = client.newCall(Request.Builder().url(server.url("/health")).build()).execute()
        assertEquals(200, response.code)
        assertEquals("still-here", response.body?.string())
    }
}
