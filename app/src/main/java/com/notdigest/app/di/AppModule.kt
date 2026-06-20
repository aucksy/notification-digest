package com.notdigest.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import com.notdigest.app.core.Constants
import com.notdigest.app.data.local.NotDigestDatabase
import com.notdigest.app.data.local.dao.AppRuleDao
import com.notdigest.app.data.local.dao.DigestDao
import com.notdigest.app.data.local.dao.DismissedRecommendationDao
import com.notdigest.app.data.local.dao.NotificationDao
import com.notdigest.app.data.local.dao.ScheduleDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

/** Provides framework-backed singletons: the database, DAOs, DataStore, dispatchers and scope. */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NotDigestDatabase =
        Room.databaseBuilder(context, NotDigestDatabase::class.java, Constants.DATABASE_NAME)
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()

    @Provides
    fun provideNotificationDao(db: NotDigestDatabase): NotificationDao = db.notificationDao()

    @Provides
    fun provideDigestDao(db: NotDigestDatabase): DigestDao = db.digestDao()

    @Provides
    fun provideAppRuleDao(db: NotDigestDatabase): AppRuleDao = db.appRuleDao()

    @Provides
    fun provideScheduleDao(db: NotDigestDatabase): ScheduleDao = db.scheduleDao()

    @Provides
    fun provideDismissedRecommendationDao(db: NotDigestDatabase): DismissedRecommendationDao =
        db.dismissedRecommendationDao()

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile(Constants.DATASTORE_NAME) },
        )

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(@DefaultDispatcher dispatcher: CoroutineDispatcher): CoroutineScope =
        CoroutineScope(SupervisorJob() + dispatcher)
}
