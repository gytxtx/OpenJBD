package com.gytxtx.openjbd

import android.app.Activity
import android.content.res.Configuration
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager

internal object SystemBars {
    @JvmStatic
    fun applyAppBars(activity: Activity) {
        val window = activity.window
        val darkTheme = isDarkTheme(activity)
        window.statusBarColor = activity.getColor(R.color.primary_dark)
        window.navigationBarColor = activity.getColor(R.color.surface)
        if (Build.VERSION.SDK_INT >= 29) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
        if (Build.VERSION.SDK_INT >= 30) {
            val controller = window.decorView.windowInsetsController
            if (controller != null) {
                val appearance = if (darkTheme)
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                else
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                controller.setSystemBarsAppearance(
                    appearance,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                            or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                )
            } else {
                applyLegacyBarIcons(window, darkTheme)
            }
        } else {
            applyLegacyBarIcons(window, darkTheme)
        }
    }

    @JvmStatic
    fun applyFullscreen(activity: Activity) {
        val window = activity.window
        @Suppress("DEPRECATION")
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        if (Build.VERSION.SDK_INT >= 30) {
            val controller = window.decorView.windowInsetsController
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                applyLegacyFullscreen(window)
            }
        } else {
            applyLegacyFullscreen(window)
        }
    }

    private fun isDarkTheme(activity: Activity): Boolean {
        val nightMode =
            activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return nightMode == Configuration.UI_MODE_NIGHT_YES
    }

    private fun applyLegacyBarIcons(window: Window, darkTheme: Boolean) {
        var flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        if (darkTheme && Build.VERSION.SDK_INT >= 23) {
            flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
        if (!darkTheme && Build.VERSION.SDK_INT >= 26) {
            flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = flags
    }

    @Suppress("DEPRECATION")
    private fun applyLegacyFullscreen(window: Window) {
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                )
    }
}
