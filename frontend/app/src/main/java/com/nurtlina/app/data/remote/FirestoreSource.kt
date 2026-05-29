package com.nurtlina.app.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.nurtlina.app.data.remote.dto.RemoteBabyDto
import com.nurtlina.app.data.remote.dto.RemoteBottleDto
import com.nurtlina.app.data.remote.dto.RemoteDiaperLogDto
import com.nurtlina.app.data.remote.dto.RemoteFeedLogDto
import com.nurtlina.app.data.remote.dto.RemoteSleepLogDto
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Low-level Firestore read/write operations.
 *
 * All writes use [SetOptions.merge] so partial updates don't wipe fields that
 * the server may have set (e.g. entitlement flags).
 *
 * Conflict strategy: the caller is responsible for checking updatedAt before
 * calling upsert. The Firestore security rule also enforces isNotStale().
 */
@Singleton
class FirestoreSource @Inject constructor(
    private val firestore: FirebaseFirestore,
) {

    // ── Babies ────────────────────────────────────────────────────────────────

    suspend fun upsertBaby(familyId: String, dto: RemoteBabyDto) {
        familyCollection(familyId, BABIES)
            .document(dto.id)
            .set(dto.toMap(), SetOptions.merge())
            .await()
    }

    suspend fun fetchBabiesSince(familyId: String, sinceMillis: Long): List<RemoteBabyDto> =
        familyCollection(familyId, BABIES)
            .whereGreaterThan("updatedAt", sinceMillis)
            .get()
            .await()
            .documents
            .mapNotNull { it.data?.let(RemoteBabyDto::fromMap) }

    // ── Bottles ───────────────────────────────────────────────────────────────

    suspend fun upsertBottle(familyId: String, dto: RemoteBottleDto) {
        familyCollection(familyId, BOTTLES)
            .document(dto.id)
            .set(dto.toMap(), SetOptions.merge())
            .await()
    }

    suspend fun fetchBottlesSince(familyId: String, sinceMillis: Long): List<RemoteBottleDto> =
        familyCollection(familyId, BOTTLES)
            .whereGreaterThan("updatedAt", sinceMillis)
            .get()
            .await()
            .documents
            .mapNotNull { it.data?.let(RemoteBottleDto::fromMap) }

    suspend fun fetchBottle(familyId: String, bottleId: String): RemoteBottleDto? =
        familyCollection(familyId, BOTTLES)
            .document(bottleId)
            .get()
            .await()
            .data
            ?.let(RemoteBottleDto::fromMap)

    // ── FeedLogs ──────────────────────────────────────────────────────────────

    suspend fun upsertFeedLog(familyId: String, dto: RemoteFeedLogDto) {
        familyCollection(familyId, FEED_LOGS)
            .document(dto.id)
            .set(dto.toMap(), SetOptions.merge())
            .await()
    }

    suspend fun fetchFeedLogsSince(familyId: String, sinceMillis: Long): List<RemoteFeedLogDto> =
        familyCollection(familyId, FEED_LOGS)
            .whereGreaterThan("updatedAt", sinceMillis)
            .get()
            .await()
            .documents
            .mapNotNull { it.data?.let(RemoteFeedLogDto::fromMap) }

    // ── DiaperLogs ────────────────────────────────────────────────────────────

    suspend fun upsertDiaperLog(familyId: String, dto: RemoteDiaperLogDto) {
        familyCollection(familyId, DIAPER_LOGS)
            .document(dto.id)
            .set(dto.toMap(), SetOptions.merge())
            .await()
    }

    suspend fun fetchDiaperLogsSince(familyId: String, sinceMillis: Long): List<RemoteDiaperLogDto> =
        familyCollection(familyId, DIAPER_LOGS)
            .whereGreaterThan("updatedAt", sinceMillis)
            .get()
            .await()
            .documents
            .mapNotNull { it.data?.let(RemoteDiaperLogDto::fromMap) }

    // ── SleepLogs ─────────────────────────────────────────────────────────────

    suspend fun upsertSleepLog(familyId: String, dto: RemoteSleepLogDto) {
        familyCollection(familyId, SLEEP_LOGS)
            .document(dto.id)
            .set(dto.toMap(), SetOptions.merge())
            .await()
    }

    suspend fun fetchSleepLogsSince(familyId: String, sinceMillis: Long): List<RemoteSleepLogDto> =
        familyCollection(familyId, SLEEP_LOGS)
            .whereGreaterThan("updatedAt", sinceMillis)
            .get()
            .await()
            .documents
            .mapNotNull { it.data?.let(RemoteSleepLogDto::fromMap) }

    // ── User / Family ─────────────────────────────────────────────────────────

    suspend fun fetchFamilyId(userId: String): String? =
        firestore.collection(USERS)
            .document(userId)
            .get()
            .await()
            .getString("familyId")

    suspend fun fetchEntitlement(userId: String): Boolean =
        firestore.collection(ENTITLEMENTS)
            .document(userId)
            .get()
            .await()
            .getBoolean("isProActive") ?: false

    // ── Internal helpers ──────────────────────────────────────────────────────

    private fun familyCollection(familyId: String, sub: String) =
        firestore.collection(FAMILIES).document(familyId).collection(sub)

    companion object {
        const val USERS = "users"
        const val FAMILIES = "families"
        const val ENTITLEMENTS = "entitlements"
        const val BABIES = "babies"
        const val BOTTLES = "bottles"
        const val FEED_LOGS = "feedLogs"
        const val DIAPER_LOGS = "diaperLogs"
        const val SLEEP_LOGS = "sleepLogs"
    }
}
