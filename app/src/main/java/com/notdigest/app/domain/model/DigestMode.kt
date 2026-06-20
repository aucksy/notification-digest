package com.notdigest.app.domain.model

/**
 * How an app's notifications are handled.
 *
 * - [DIGEST]   collected silently and delivered in scheduled batches.
 * - [REALTIME] passed straight through, exactly as Android delivered them.
 */
enum class DigestMode {
    DIGEST,
    REALTIME;

    fun toggled(): DigestMode = if (this == DIGEST) REALTIME else DIGEST
}
