package com.quizmaker.android.ui.common

import android.content.Context
import android.os.Build
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
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
 *
 * `setBackgroundBlurRadius` itself silently renders nothing whenever the OS's cross-window blur
 * capability is off — `WindowManager.isCrossWindowBlurEnabled` — which happens per-device/per-state
 * (Battery Saver, the "Disable HW overlays"/"Force 4x MSAA" developer options, some OEM skins and
 * emulator GPUs that never support it at all) independent of API level, so a real Android 16 device
 * can still show nothing. That's the exact case Android's own docs for this API warn about
 * ("always set a background color or a dim amount... to ensure proper contrast when the blur can't
 * be applied") — so this falls back to a plain dim scrim instead of leaving the background
 * untouched, which is what a silent no-op looked like before this fallback existed.
 */
@Composable
fun BlurBehindDialog(radius: Dp = 24.dp) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val view = LocalView.current
    val density = LocalDensity.current
    val context = LocalContext.current
    SideEffect {
        val window = (view.parent as? DialogWindowProvider)?.window ?: return@SideEffect
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        if (windowManager?.isCrossWindowBlurEnabled != true) {
            window.setDimAmount(0.45f)
            return@SideEffect
        }
        val radiusPx = with(density) { radius.toPx() }.toInt().coerceAtLeast(1)
        window.setBackgroundBlurRadius(radiusPx)
    }
}
