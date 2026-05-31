package com.nurtlina.app.data.billing

import java.time.Instant

data class EntitlementCache(
    val isPro: Boolean,
    val plan: String?,
    val status: String?,
    val expiresAt: Instant?,
    val lastVerifiedAt: Instant?,
    val gracePeriodUntil: Instant?,
    val source: String?,
)
