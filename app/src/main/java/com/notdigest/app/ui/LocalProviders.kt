package com.notdigest.app.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.notdigest.app.data.system.AppIconLoader

/** Whether to render times in 24-hour format (from the device setting). */
val LocalIs24Hour = staticCompositionLocalOf { false }

/** App-icon loader, provided at the activity root so any composable can render app icons. */
val LocalAppIconLoader = staticCompositionLocalOf<AppIconLoader?> { null }
