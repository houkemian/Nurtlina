package com.nurtlina.app.domain.repository

import com.nurtlina.app.domain.model.SyncState
import kotlinx.coroutines.flow.Flow

/**
 * Orchestrates upload and download of local records to/from Firestore.
 *
 * Rules:
 * - Sync must never block bottle timer creation, state changes, or notifications.
 * - Sync failures must degrade gracefully; the app remains fully usable offline.
 * - Remote updates must not override newer local state (conflict: last-updatedAt wins).
 * - Soft-deleted records (deletedAt != null) are pushed and then purged locally.
 */
interface SyncRepository {

    /** Emits the current sync state (syncing flag, last sync time, last error). */
    fun observeSyncState(): Flow<SyncState>

    /**
     * Pushes all local records modified since [since] to Firestore and pulls
     * remote changes. Returns success or a wrapped exception on failure.
     * Should be called by [SyncWorker] rather than directly from the UI.
     */
    suspend fun syncAll(): Result<Unit>

    /**
     * Uploads a single record immediately after a local mutation. Used to
     * reduce latency for individual writes when online. Falls back silently
     * if the network is unavailable.
     */
    suspend fun syncRecord(collectionName: String, id: String): Result<Unit>

    /**
     * Marks local sync state as needing a full pull on the next sync cycle.
     * Called after sign-in to ensure the device downloads the user's cloud data.
     */
    suspend fun requestFullSync()

    /** Clears cached sync timestamps, forcing a full re-sync on next cycle. */
    suspend fun resetSyncState()
}
