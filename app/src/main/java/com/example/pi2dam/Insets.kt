package com.example.pi2dam

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

fun View.applySystemBarsPadding() {
    val startLeft = paddingLeft
    val startTop = paddingTop
    val startRight = paddingRight
    val startBottom = paddingBottom

    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        v.setPadding(
            startLeft + systemBars.left,
            startTop + systemBars.top,
            startRight + systemBars.right,
            startBottom + systemBars.bottom
        )
        insets
    }
}
