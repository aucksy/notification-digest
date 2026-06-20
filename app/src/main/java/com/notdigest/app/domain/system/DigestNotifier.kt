package com.notdigest.app.domain.system

import com.notdigest.app.domain.model.DigestWithItems

/**
 * Posts the digest summary notification. Implemented in the notification layer; abstracted here
 * so domain/use-case code stays free of Android framework types and stays unit-testable.
 */
interface DigestNotifier {

    /** Post a grouped, expandable digest notification for a freshly delivered batch. */
    fun postDigest(digest: DigestWithItems)

    /** Post (or update) the quiet ongoing "collecting" status notification. */
    fun showCollectingStatus(pendingCount: Int)

    fun clearCollectingStatus()
}
