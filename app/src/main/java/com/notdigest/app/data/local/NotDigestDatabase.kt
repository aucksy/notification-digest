package com.notdigest.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.notdigest.app.data.local.dao.AppRuleDao
import com.notdigest.app.data.local.dao.DigestDao
import com.notdigest.app.data.local.dao.DismissedRecommendationDao
import com.notdigest.app.data.local.dao.NotificationDao
import com.notdigest.app.data.local.dao.ScheduleDao
import com.notdigest.app.data.local.entity.AppRuleEntity
import com.notdigest.app.data.local.entity.DigestEntity
import com.notdigest.app.data.local.entity.DismissedRecommendationEntity
import com.notdigest.app.data.local.entity.NotificationEntity
import com.notdigest.app.data.local.entity.ScheduleEntity

@Database(
    entities = [
        NotificationEntity::class,
        DigestEntity::class,
        AppRuleEntity::class,
        ScheduleEntity::class,
        DismissedRecommendationEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class NotDigestDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao
    abstract fun digestDao(): DigestDao
    abstract fun appRuleDao(): AppRuleDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun dismissedRecommendationDao(): DismissedRecommendationDao
}
