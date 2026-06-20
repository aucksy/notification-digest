package com.notdigest.app.data.local

import androidx.room.TypeConverter
import com.notdigest.app.domain.model.NotificationActionItem

/**
 * Stores the small list of notification actions as a single string using control-character
 * delimiters (Unit Separator 0x1F / Record Separator 0x1E — they never appear in action labels).
 * Avoids pulling in a JSON dependency for such a tiny payload.
 */
class Converters {

    private val itemSep: Char = Char(31)   // Unit Separator, between actions
    private val fieldSep: Char = Char(30)  // Record Separator, between index and title

    @TypeConverter
    fun fromActions(actions: List<NotificationActionItem>): String =
        actions.joinToString(itemSep.toString()) { item ->
            val safeTitle = item.title.replace(itemSep, ' ').replace(fieldSep, ' ')
            "${item.index}$fieldSep$safeTitle"
        }

    @TypeConverter
    fun toActions(raw: String): List<NotificationActionItem> {
        if (raw.isBlank()) return emptyList()
        return raw.split(itemSep).mapNotNull { token ->
            val parts = token.split(fieldSep)
            if (parts.size != 2) return@mapNotNull null
            val index = parts[0].toIntOrNull() ?: return@mapNotNull null
            NotificationActionItem(index = index, title = parts[1])
        }
    }
}
