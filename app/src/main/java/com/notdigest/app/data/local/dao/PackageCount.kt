package com.notdigest.app.data.local.dao

/** Projection for per-app aggregate counts. */
data class PackageCount(
    val packageName: String,
    val cnt: Int,
)
