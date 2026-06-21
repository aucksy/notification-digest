package com.notdigest.app

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.notdigest.app.data.system.ConfigBackupManager
import com.notdigest.app.data.system.DriveBackupManager
import com.notdigest.app.di.ApplicationScope
import com.notdigest.app.domain.system.DigestScheduler
import com.notdigest.app.domain.usecase.InitializeAppDataUseCase
import com.notdigest.app.notification.NotificationChannels
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Application entry point. Wires Hilt, provides a Hilt-aware WorkManager factory, creates
 * notification channels, runs first-launch data setup (restore/seed/migrate), and keeps the
 * cloud-backup snapshot current.
 */
@HiltAndroidApp
class NotDigestApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var channels: NotificationChannels
    @Inject lateinit var scheduler: DigestScheduler
    @Inject lateinit var initializeAppData: InitializeAppDataUseCase
    @Inject lateinit var configBackup: ConfigBackupManager
    @Inject lateinit var driveBackup: DriveBackupManager

    @Inject @ApplicationScope lateinit var appScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        channels.ensureChannels()
        scheduler.ensureCleanupScheduled()

        appScope.launch {
            // Restore (if any) must finish reading the snapshot BEFORE the backup writer starts,
            // so we never overwrite a freshly restored file.
            runCatching { initializeAppData() }
            configBackup.start(appScope)
            driveBackup.start(appScope)
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.INFO)
            .build()
}
