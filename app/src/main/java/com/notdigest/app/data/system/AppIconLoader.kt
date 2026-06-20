package com.notdigest.app.data.system

import android.content.Context
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.notdigest.app.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads & caches app launcher icons as Compose [ImageBitmap]s. Decoding happens off the main
 * thread and results are memo-cached so list scrolling stays smooth with hundreds of apps.
 */
@Singleton
class AppIconLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    private val cache = LruCache<String, ImageBitmap>(256)

    suspend fun load(packageName: String): ImageBitmap? {
        cache.get(packageName)?.let { return it }
        return withContext(io) {
            runCatching {
                val drawable = context.packageManager.getApplicationIcon(packageName)
                val bitmap = drawable.toBitmap(width = ICON_PX, height = ICON_PX).asImageBitmap()
                cache.put(packageName, bitmap)
                bitmap
            }.getOrNull()
        }
    }

    private companion object {
        const val ICON_PX = 144
    }
}
