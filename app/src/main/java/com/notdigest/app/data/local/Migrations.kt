package com.notdigest.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 → v2: adds the content-free `realtime_events` table used to estimate how noisy un-batched apps
 * are. The CREATE TABLE matches exactly what Room generates for [com.notdigest.app.data.local.entity.RealtimeEventEntity]
 * (same columns, types, NOT NULL and primary key), so Room's schema validation passes.
 */
val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `realtime_events` (" +
                "`id` INTEGER NOT NULL, " +
                "`packageName` TEXT NOT NULL, " +
                "`appName` TEXT NOT NULL, " +
                "`postedAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`))",
        )
    }
}
