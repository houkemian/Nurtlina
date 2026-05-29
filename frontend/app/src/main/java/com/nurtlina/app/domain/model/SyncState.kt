package com.nurtlina.app.domain.model

import java.time.Instant

/**
 * Tracks the last successful sync timestamp and whether a sync is pending.
 * Persisted in DataStore so the app can resume partial syncs after network returns.
 */
data class SyncState(
    val lastSyncedAt: Instant?,
    val isSyncing: Boolean,
    val lastError: String?,
)

/**
 * Sync metadata attached to every record that is mirrored to Firestore.
 * Use deterministic client-generated UUIDs for [id] so offline-created records
 * survive without server round-trips.
 */
data class SyncMetadata(
    val id: String,
    val ownerUserId: String,
    val familyId: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant?,
    val clientId: String,
    val schemaVersion: Int,
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}
