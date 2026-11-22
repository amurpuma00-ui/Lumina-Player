package com.liquidglass.player.util

import android.app.Activity
import android.os.Build
import android.view.Display
import com.liquidglass.player.data.RefreshRateMode
import kotlin.math.abs

object DisplayRefreshController {

    fun getSupportedRefreshRates(activity: Activity): List<Int> {
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.display
        } else {
            @Suppress("DEPRECATION")
            activity.windowManager.defaultDisplay
        } ?: return listOf(60)

        return display.supportedModes
            .map { it.refreshRate.toInt() }
            .distinct()
            .sorted()
    }

    fun getCurrentRefreshRate(activity: Activity): Int {
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.display
        } else {
            @Suppress("DEPRECATION")
            activity.windowManager.defaultDisplay
        } ?: return 60
        return display.refreshRate.toInt()
    }

    fun applyRefreshRate(activity: Activity, mode: RefreshRateMode): Int {
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.display
        } else {
            @Suppress("DEPRECATION")
            activity.windowManager.defaultDisplay
        } ?: return 60

        val params = activity.window.attributes
        val currentMode = display.mode
        val supportedModes: Array<Display.Mode> = display.supportedModes

        // Filter modes matching current screen resolution so we never alter scaling
        val sameResModes = supportedModes.filter {
            it.physicalWidth == currentMode.physicalWidth && it.physicalHeight == currentMode.physicalHeight
        }.ifEmpty { supportedModes.toList() }

        when (mode) {
            RefreshRateMode.AUTO -> {
                params.preferredDisplayModeId = 0
                params.preferredRefreshRate = 0f
                activity.window.attributes = params
                return display.refreshRate.toInt()
            }
            RefreshRateMode.MAX -> {
                val best = sameResModes.maxByOrNull { it.refreshRate }
                if (best != null) {
                    params.preferredDisplayModeId = best.modeId
                    params.preferredRefreshRate = best.refreshRate
                    activity.window.attributes = params
                    return best.refreshRate.toInt()
                }
            }
            else -> {
                val target = mode.targetHz
                val closest = sameResModes.minByOrNull { abs(it.refreshRate - target) }
                if (closest != null) {
                    params.preferredDisplayModeId = closest.modeId
                    params.preferredRefreshRate = closest.refreshRate
                    activity.window.attributes = params
                    return closest.refreshRate.toInt()
                }
            }
        }
        return display.refreshRate.toInt()
    }
}
