package com.nurtlina.app.data.sync

object SyncEntityTypes {
    const val BABY = "BABY"
    const val BOTTLE = "BOTTLE"
    const val FEED_LOG = "FEED_LOG"
    const val DIAPER_LOG = "DIAPER_LOG"
    const val SLEEP_LOG = "SLEEP_LOG"
    const val SETTINGS = "SETTINGS"
}

object SyncOperations {
    const val UPSERT_BABY = "UPSERT_BABY"
    const val DELETE_BABY = "DELETE_BABY"
    const val UPSERT_BOTTLE = "UPSERT_BOTTLE"
    const val DELETE_BOTTLE = "DELETE_BOTTLE"
    const val UPSERT_FEED_LOG = "UPSERT_FEED_LOG"
    const val DELETE_FEED_LOG = "DELETE_FEED_LOG"
    const val UPSERT_DIAPER_LOG = "UPSERT_DIAPER_LOG"
    const val DELETE_DIAPER_LOG = "DELETE_DIAPER_LOG"
    const val UPSERT_SLEEP_LOG = "UPSERT_SLEEP_LOG"
    const val DELETE_SLEEP_LOG = "DELETE_SLEEP_LOG"
    const val UPDATE_SETTINGS = "UPDATE_SETTINGS"
}
