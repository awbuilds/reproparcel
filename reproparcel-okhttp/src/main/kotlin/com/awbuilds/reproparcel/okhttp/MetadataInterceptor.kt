// SPDX-License-Identifier: AGPL-3.0-only
package com.awbuilds.reproparcel.okhttp

import com.awbuilds.reproparcel.core.model.NetworkMetadata
import okhttp3.Interceptor
import okhttp3.Response

public class RouteTemplate(public val value: String)

/**
 * Opt-in metadata interceptor. It records only method, status, elapsed time and
 * a host-supplied safe route template. It never reads bodies, headers, query
 * strings or raw URLs, never consumes the response stream and never changes the
 * host response or exception.
 */
public class MetadataInterceptor(
    private val sink: (NetworkMetadata) -> Unit,
    private val onSinkFailure: ((String) -> Unit)? = null,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val route = request.tag(RouteTemplate::class.java)?.value
        val started = System.nanoTime()
        val response = chain.proceed(request)
        val elapsedMs = (System.nanoTime() - started) / 1_000_000L
        try {
            sink(
                NetworkMetadata(
                    method = request.method,
                    statusCode = response.code,
                    durationMs = elapsedMs.coerceAtLeast(0),
                    routeTemplate = route,
                ),
            )
        } catch (failure: Exception) {
            // The sink is a host callback. Its failure must not alter the
            // network response; report only a safe class name.
            onSinkFailure?.invoke(failure.javaClass.simpleName)
        }
        return response
    }
}
