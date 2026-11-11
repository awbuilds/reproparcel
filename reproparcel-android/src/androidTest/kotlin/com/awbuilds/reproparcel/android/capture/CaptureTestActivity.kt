// SPDX-License-Identifier: Apache-2.0
package com.awbuilds.reproparcel.android.capture

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.WindowManager

class CaptureTestActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(
            View(this).apply {
                setBackgroundColor(android.graphics.Color.rgb(20, 40, 60))
            },
        )
    }

    fun markSecure() {
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}
