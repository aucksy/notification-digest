package com.notdigest.app.core.util

import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** Abstracts the wall clock so time-dependent logic can be unit-tested deterministically. */
interface TimeProvider {
    fun now(): Long
    fun zone(): ZoneId
}

@Singleton
class SystemTimeProvider @Inject constructor() : TimeProvider {
    override fun now(): Long = System.currentTimeMillis()
    override fun zone(): ZoneId = ZoneId.systemDefault()
}
