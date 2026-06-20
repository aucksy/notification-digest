package com.notdigest.app.domain.repository

import com.notdigest.app.domain.model.InstalledApp

/** Reads the set of launchable installed apps from the platform. */
interface InstalledAppsRepository {

    /** Launchable apps (excludes this app), each joined with its current rule. */
    suspend fun getInstalledApps(): List<InstalledApp>

    /** Human-readable label for a package, falling back to the package name. */
    suspend fun appLabel(packageName: String): String
}
