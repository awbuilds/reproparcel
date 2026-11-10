// SPDX-License-Identifier: AGPL-3.0-only
package com.awbuilds.reproparcel.android.device

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.DisplayMetrics
import com.awbuilds.reproparcel.core.model.DeviceInfo
import java.util.Locale

public object DeviceInfoProvider {
    public fun collect(context: Context): DeviceInfo {
        val configuration: Configuration = context.resources.configuration
        val metrics = context.resources.displayMetrics
        val width = metrics.widthPixels.takeIf { it > 0 } ?: configuration.screenWidthDp
        val height = metrics.heightPixels.takeIf { it > 0 } ?: configuration.screenHeightDp
        val orientation = when (configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> "landscape"
            Configuration.ORIENTATION_PORTRAIT -> "portrait"
            else -> "unknown"
        }
        return DeviceInfo(
            apiLevel = Build.VERSION.SDK_INT,
            manufacturer = Build.MANUFACTURER.take(80),
            model = Build.MODEL.take(80),
            windowWidthPx = width.coerceAtLeast(1),
            windowHeightPx = height.coerceAtLeast(1),
            densityDpi = configuration.densityDpi.coerceAtLeast(1),
            locale = Locale.getDefault().toLanguageTag().take(32),
            fontScale = configuration.fontScale.coerceIn(0.5f, 4f),
            orientation = orientation,
        )
    }
}
