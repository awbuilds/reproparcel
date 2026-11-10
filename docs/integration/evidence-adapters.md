# Evidence adapter integration

## Breadcrumbs

```kotlin
ReproParcel.recordBreadcrumb(
    Breadcrumb(
        category = "navigation",
        actionId = "open_checkout",
        screenId = "checkout_review",
        attributes = mapOf(
            "screen_id" to "checkout_review",
            "route_template" to "/checkout/{id}",
        ),
    ),
)
```

Do not pass navigation arguments, form values, query strings, tokens or raw
URLs. Unknown attributes are dropped.

## Structured logs

```kotlin
ReproParcel.recordLog(
    LogEvent(
        level = "warn",
        code = "save_no_effect",
        attributes = mapOf("component" to "checkout"),
    ),
)
```

Use a stable safe code, not an exception message. Limit: 200 events and 256 KiB;
both apply.

## OkHttp metadata

```kotlin
val client = OkHttpClient.Builder()
    .addInterceptor(
        MetadataInterceptor(
            sink = { metadata ->
                ReproParcel.recordNetworkMetadata(metadata)
            },
        ),
    )
    .build()
```

Attach a route template to the request:

```kotlin
Request.Builder()
    .url(url)
    .tag(RouteTemplate::class.java, RouteTemplate("/orders/{id}"))
    .build()
```

Never install this interceptor on the SDK's own client. Bodies, headers, query
strings and raw URLs are not recorded. The host response and exception are
preserved unchanged.

## Explicit semantics

Register only reviewed descriptors:

```kotlin
ReproParcel.registerSemanticNode(
    SemanticNode(
        nodeId = "save_button",
        role = "button",
        label = "Save",
        testTag = "save_button",
        bounds = Bounds(100, 1400, 980, 1500),
    ),
)
```

Mark sensitive nodes with `sensitive = true`; label, test tag and state are
discarded. Do not rely on automatic Compose tree extraction in v1.

## Limits

| Evidence | Limit |
|---|---|
| Breadcrumbs | 50 events / 20 actions, 5 minutes |
| Logs | 200 events, 256 KiB |
| Network | 50 entries, 128 KiB |
| Semantics | 500 nodes, depth 32, 512 KiB |
| Runtime evidence buffer | 1 MiB combined |
