package com.android.girish.vlog

import com.facebook.rebound.SpringConfig

internal object SpringConfigs {
    val NOT_DRAGGING: SpringConfig = SpringConfig.fromOrigamiTensionAndFriction(60.0, 8.0)
    val CAPTURING: SpringConfig = SpringConfig.fromBouncinessAndSpeed(8.0, 40.0)
    val CLOSE_SCALE: SpringConfig = SpringConfig.fromBouncinessAndSpeed(7.0, 25.0)
    val CLOSE_Y: SpringConfig = SpringConfig.fromBouncinessAndSpeed(3.0, 3.0)
    val DRAGGING: SpringConfig = SpringConfig.fromOrigamiTensionAndFriction(0.0, 5.0)
    val CONTENT_SCALE: SpringConfig = SpringConfig.fromBouncinessAndSpeed(5.0, 40.0)
}
