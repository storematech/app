package com.quizmaker.android.ui.common

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider

/**
 * Blurs whatever is visible behind this popup's own window — the iOS-style frosted background
 * instead of Android's default flat dim scrim. Call it as the first thing inside a
 * `Dialog`/`AlertDialog`/`ModalBottomSheet`'s content: their content view is what actually owns
 * the window here, via `DialogWindowProvider` — Compose's own `Dialog` and Material3's
 * `ModalBottomSheet` both host their content in a view that implements it, so
 * `LocalView.current.parent` resolves to it from inside the content lambda.
 *
 * No-op below Android 12 (API 31) — `Window.setBackgroundBlurRadius` doesn't exist before that,
 * so older devices just keep the plain dim scrim they already had; nothing to blur with, nothing
 * broken either. [radius] defaults to a subtle amount (not the heavy full-screen blur some Android
 * "blur libraries" produce) to match the light iOS-style frosting this was asked for.
 */
@Composable
fun BlurBehindDialog(radius: Dp = 24.dp) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val view = LocalView.current
    val density = LocalDensity.current
    SideEffect {
        val window = (view.parent as? DialogWindowProvider)?.window ?: return@SideEffect
        val radiusPx = with(density) { radius.toPx() }.toInt().coerceAtLeast(1)
        window.setBackgroundBlurRadius(radiusPx)
    }
}
