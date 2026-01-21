// UiUtils.kt
package com.tuapp.utils

import android.app.Activity
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

fun Activity.hideNavBar(bottomNav: BottomNavigationView) {
    val insetsController = WindowInsetsControllerCompat(window, window.decorView)

    insetsController.hide(WindowInsetsCompat.Type.navigationBars())
    insetsController.systemBarsBehavior =
        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

    // Rehabilitar interacción de tu navbar
    bottomNav.isEnabled = true
    bottomNav.alpha = 1f
}
