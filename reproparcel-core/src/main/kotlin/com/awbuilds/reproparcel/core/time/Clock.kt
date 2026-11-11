// SPDX-License-Identifier: Apache-2.0
package com.awbuilds.reproparcel.core.time

public fun interface Clock {
    public fun nowMs(): Long
}

public object SystemClock : Clock {
    override fun nowMs(): Long = System.currentTimeMillis()
}
