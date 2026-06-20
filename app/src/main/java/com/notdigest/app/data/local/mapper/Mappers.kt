package com.notdigest.app.data.local.mapper

import com.notdigest.app.data.local.entity.AppRuleEntity
import com.notdigest.app.data.local.entity.DigestEntity
import com.notdigest.app.data.local.entity.NotificationEntity
import com.notdigest.app.data.local.entity.ScheduleEntity
import com.notdigest.app.domain.model.AppNotification
import com.notdigest.app.domain.model.AppRule
import com.notdigest.app.domain.model.Digest
import com.notdigest.app.domain.model.DigestMode
import com.notdigest.app.domain.model.DigestType
import com.notdigest.app.domain.model.Schedule

// --- Notifications ---

fun NotificationEntity.toDomain(): AppNotification = AppNotification(
    id = id,
    sbnKey = sbnKey,
    packageName = packageName,
    appName = appName,
    title = title,
    text = text,
    subText = subText,
    category = category,
    postedAt = postedAt,
    isRead = isRead,
    isDelivered = isDelivered,
    digestId = digestId,
    hasDeepLink = hasDeepLink,
    actions = actions,
)

fun AppNotification.toEntity(): NotificationEntity = NotificationEntity(
    id = id,
    sbnKey = sbnKey,
    packageName = packageName,
    appName = appName,
    title = title,
    text = text,
    subText = subText,
    category = category,
    postedAt = postedAt,
    isRead = isRead,
    isDelivered = isDelivered,
    digestId = digestId,
    deliveredAt = if (isDelivered) postedAt else null,
    hasDeepLink = hasDeepLink,
    actions = actions,
)

// --- Digests ---

fun DigestEntity.toDomain(): Digest = Digest(
    id = id,
    createdAt = createdAt,
    type = runCatching { DigestType.valueOf(type) }.getOrDefault(DigestType.SCHEDULED),
    notificationCount = notificationCount,
    appCount = appCount,
)

// --- App rules ---

fun AppRuleEntity.toDomain(): AppRule = AppRule(
    packageName = packageName,
    appName = appName,
    mode = runCatching { DigestMode.valueOf(mode) }.getOrDefault(DigestMode.DIGEST),
    isSystemApp = isSystemApp,
    updatedAt = updatedAt,
)

fun AppRule.toEntity(): AppRuleEntity = AppRuleEntity(
    packageName = packageName,
    appName = appName,
    mode = mode.name,
    isSystemApp = isSystemApp,
    updatedAt = updatedAt,
)

// --- Schedules ---

fun ScheduleEntity.toDomain(): Schedule = Schedule(
    id = id,
    label = label,
    minuteOfDay = minuteOfDay,
    enabled = enabled,
    sortOrder = sortOrder,
)

fun Schedule.toEntity(): ScheduleEntity = ScheduleEntity(
    id = id,
    label = label,
    minuteOfDay = minuteOfDay,
    enabled = enabled,
    sortOrder = sortOrder,
)
