package com.nurtlina.app.data.remote.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface BackendApiService {
    @POST("api/v1/me/init")
    suspend fun initMe(@Body request: MeInitRequest): MeInitResponse

    @POST("api/v1/sync/babies")
    suspend fun pushBabies(@Body request: SyncPushRequest<BabyChangeDto>): SyncPushResponse

    @POST("api/v1/sync/bottles")
    suspend fun pushBottles(@Body request: SyncPushRequest<BottleChangeDto>): SyncPushResponse

    @POST("api/v1/sync/feed-logs")
    suspend fun pushFeedLogs(@Body request: SyncPushRequest<FeedLogChangeDto>): SyncPushResponse

    @POST("api/v1/sync/diaper-logs")
    suspend fun pushDiaperLogs(@Body request: SyncPushRequest<DiaperLogChangeDto>): SyncPushResponse

    @POST("api/v1/sync/sleep-logs")
    suspend fun pushSleepLogs(@Body request: SyncPushRequest<SleepLogChangeDto>): SyncPushResponse

    @GET("api/v1/sync/changes")
    suspend fun pullChanges(
        @Query("family_id") familyId: String,
        @Query("client_id") clientId: String,
        @Query("since") since: String,
    ): SyncPullResponse

    @POST("api/v1/billing/google-play/purchases")
    suspend fun submitPurchase(@Body request: PurchaseVerificationRequest): EntitlementResponse

    @GET("api/v1/billing/entitlements/me")
    suspend fun getEntitlement(): EntitlementResponse
}

data class MeInitRequest(
    @SerializedName("clientId") val clientId: String,
    @SerializedName("appVersion") val appVersion: String,
    @SerializedName("platform") val platform: String = "ANDROID",
)

data class MeInitResponse(
    @SerializedName("user_id") val userId: String,
    @SerializedName("default_family_id") val defaultFamilyId: String,
    @SerializedName("is_new_user") val isNewUser: Boolean,
)

data class SyncPushRequest<T>(
    @SerializedName("family_id") val familyId: String,
    @SerializedName("client_id") val clientId: String,
    @SerializedName("changes") val changes: List<T>,
)

data class SyncPushResponse(
    @SerializedName("server_time") val serverTime: String,
    @SerializedName("accepted") val accepted: List<String>,
    @SerializedName("rejected") val rejected: List<String>,
    @SerializedName("conflicts") val conflicts: List<String>,
)

data class SyncPullResponse(
    @SerializedName("server_time") val serverTime: String,
    @SerializedName("next_cursor") val nextCursor: String?,
    @SerializedName("has_more") val hasMore: Boolean,
    @SerializedName("babies") val babies: List<BabyChangeDto>,
    @SerializedName("bottles") val bottles: List<BottleChangeDto>,
    @SerializedName("feed_logs") val feedLogs: List<FeedLogChangeDto>,
    @SerializedName("diaper_logs") val diaperLogs: List<DiaperLogChangeDto>,
    @SerializedName("sleep_logs") val sleepLogs: List<SleepLogChangeDto>,
)

data class BabyChangeDto(
    @SerializedName("id") val id: String,
    @SerializedName("family_id") val familyId: String,
    @SerializedName("name") val name: String,
    @SerializedName("birth_date") val birthDate: String?,
    @SerializedName("avatar_color") val avatarColor: String?,
    @SerializedName("client_id") val clientId: String?,
    @SerializedName("schema_version") val schemaVersion: Int,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String,
    @SerializedName("deleted_at") val deletedAt: String?,
)

data class BottleChangeDto(
    @SerializedName("id") val id: String,
    @SerializedName("family_id") val familyId: String,
    @SerializedName("baby_id") val babyId: String,
    @SerializedName("milk_type") val milkType: String,
    @SerializedName("amount_ml") val amountMl: Double?,
    @SerializedName("prepared_at") val preparedAt: String,
    @SerializedName("feeding_started_at") val feedingStartedAt: String?,
    @SerializedName("refrigerated_at") val refrigeratedAt: String?,
    @SerializedName("status") val status: String,
    @SerializedName("guideline_region") val guidelineRegion: String,
    @SerializedName("rule_version") val ruleVersion: String,
    @SerializedName("expires_at") val expiresAt: String?,
    @SerializedName("discarded_at") val discardedAt: String?,
    @SerializedName("fed_at") val fedAt: String?,
    @SerializedName("note") val note: String?,
    @SerializedName("client_id") val clientId: String?,
    @SerializedName("schema_version") val schemaVersion: Int,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String,
    @SerializedName("deleted_at") val deletedAt: String?,
)

data class FeedLogChangeDto(
    @SerializedName("id") val id: String,
    @SerializedName("family_id") val familyId: String,
    @SerializedName("baby_id") val babyId: String,
    @SerializedName("bottle_id") val bottleId: String?,
    @SerializedName("feed_type") val feedType: String,
    @SerializedName("amount_ml") val amountMl: Double?,
    @SerializedName("started_at") val startedAt: String,
    @SerializedName("ended_at") val endedAt: String?,
    @SerializedName("note") val note: String?,
    @SerializedName("client_id") val clientId: String?,
    @SerializedName("schema_version") val schemaVersion: Int,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String,
    @SerializedName("deleted_at") val deletedAt: String?,
)

data class DiaperLogChangeDto(
    @SerializedName("id") val id: String,
    @SerializedName("family_id") val familyId: String,
    @SerializedName("baby_id") val babyId: String,
    @SerializedName("diaper_type") val diaperType: String,
    @SerializedName("changed_at") val changedAt: String,
    @SerializedName("note") val note: String?,
    @SerializedName("client_id") val clientId: String?,
    @SerializedName("schema_version") val schemaVersion: Int,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String,
    @SerializedName("deleted_at") val deletedAt: String?,
)

data class SleepLogChangeDto(
    @SerializedName("id") val id: String,
    @SerializedName("family_id") val familyId: String,
    @SerializedName("baby_id") val babyId: String,
    @SerializedName("started_at") val startedAt: String,
    @SerializedName("ended_at") val endedAt: String?,
    @SerializedName("note") val note: String?,
    @SerializedName("client_id") val clientId: String?,
    @SerializedName("schema_version") val schemaVersion: Int,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String,
    @SerializedName("deleted_at") val deletedAt: String?,
)

data class PurchaseVerificationRequest(
    @SerializedName("product_id") val productId: String,
    @SerializedName("purchase_token") val purchaseToken: String,
    @SerializedName("package_name") val packageName: String,
)

data class EntitlementResponse(
    @SerializedName("is_pro") val isPro: Boolean,
    @SerializedName("plan") val plan: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("expires_at") val expiresAt: String?,
    @SerializedName("grace_period_until") val gracePeriodUntil: String?,
    @SerializedName("source") val source: String?,
    @SerializedName("last_verified_at") val lastVerifiedAt: String?,
)
