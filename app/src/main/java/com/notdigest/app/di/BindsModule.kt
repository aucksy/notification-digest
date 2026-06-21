package com.notdigest.app.di

import com.notdigest.app.core.util.SystemTimeProvider
import com.notdigest.app.core.util.TimeProvider
import com.notdigest.app.data.repository.AppRuleRepositoryImpl
import com.notdigest.app.data.repository.DigestRepositoryImpl
import com.notdigest.app.data.repository.NotificationRepositoryImpl
import com.notdigest.app.data.repository.PreferencesRepositoryImpl
import com.notdigest.app.data.repository.RealtimeStatsRepositoryImpl
import com.notdigest.app.data.repository.RecommendationRepositoryImpl
import com.notdigest.app.data.repository.ScheduleRepositoryImpl
import com.notdigest.app.data.repository.StatsRepositoryImpl
import com.notdigest.app.data.system.DeepLinkLauncherImpl
import com.notdigest.app.data.system.InstalledAppsRepositoryImpl
import com.notdigest.app.domain.repository.AppRuleRepository
import com.notdigest.app.domain.repository.DigestRepository
import com.notdigest.app.domain.repository.InstalledAppsRepository
import com.notdigest.app.domain.repository.NotificationRepository
import com.notdigest.app.domain.repository.PreferencesRepository
import com.notdigest.app.domain.repository.RealtimeStatsRepository
import com.notdigest.app.domain.repository.RecommendationRepository
import com.notdigest.app.domain.repository.ScheduleRepository
import com.notdigest.app.domain.repository.StatsRepository
import com.notdigest.app.domain.system.DeepLinkLauncher
import com.notdigest.app.domain.system.DigestNotifier
import com.notdigest.app.domain.system.DigestScheduler
import com.notdigest.app.notification.DigestNotifierImpl
import com.notdigest.app.work.DigestSchedulerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds interface contracts to their implementations. */
@Module
@InstallIn(SingletonComponent::class)
abstract class BindsModule {

    @Binds
    @Singleton
    abstract fun bindTimeProvider(impl: SystemTimeProvider): TimeProvider

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository

    @Binds
    @Singleton
    abstract fun bindDigestRepository(impl: DigestRepositoryImpl): DigestRepository

    @Binds
    @Singleton
    abstract fun bindAppRuleRepository(impl: AppRuleRepositoryImpl): AppRuleRepository

    @Binds
    @Singleton
    abstract fun bindScheduleRepository(impl: ScheduleRepositoryImpl): ScheduleRepository

    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(impl: PreferencesRepositoryImpl): PreferencesRepository

    @Binds
    @Singleton
    abstract fun bindStatsRepository(impl: StatsRepositoryImpl): StatsRepository

    @Binds
    @Singleton
    abstract fun bindRecommendationRepository(impl: RecommendationRepositoryImpl): RecommendationRepository

    @Binds
    @Singleton
    abstract fun bindRealtimeStatsRepository(impl: RealtimeStatsRepositoryImpl): RealtimeStatsRepository

    @Binds
    @Singleton
    abstract fun bindInstalledAppsRepository(impl: InstalledAppsRepositoryImpl): InstalledAppsRepository

    @Binds
    @Singleton
    abstract fun bindDeepLinkLauncher(impl: DeepLinkLauncherImpl): DeepLinkLauncher

    @Binds
    @Singleton
    abstract fun bindDigestNotifier(impl: DigestNotifierImpl): DigestNotifier

    @Binds
    @Singleton
    abstract fun bindDigestScheduler(impl: DigestSchedulerImpl): DigestScheduler
}
