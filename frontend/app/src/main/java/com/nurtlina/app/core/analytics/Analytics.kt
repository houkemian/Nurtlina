package com.nurtlina.app.core.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Analytics @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val firebase by lazy { FirebaseAnalytics.getInstance(context) }

    fun logOnboardingStarted(locale: String) = log("onboarding_started", bundle("locale" to locale))
    fun logBabyCreated() = log("baby_created", null)
    fun logGuidelineSelected(region: String) = log("guideline_selected", bundle("region" to region))
    fun logNotificationPermissionShown() = log("notification_permission_shown", null)
    fun logNotificationPermissionGranted() = log("notification_permission_granted", null)
    fun logBottleCreated(milkType: String) = log("bottle_created", bundle("milk_type" to milkType))
    fun logBottleStartedFeeding(milkType: String) = log("bottle_started_feeding", bundle("milk_type" to milkType))
    fun logBottleRefrigerated(milkType: String) = log("bottle_refrigerated", bundle("milk_type" to milkType))
    fun logBottleExpired(milkType: String) = log("bottle_expired", bundle("milk_type" to milkType))
    fun logBottleDiscarded(milkType: String) = log("bottle_discarded", bundle("milk_type" to milkType))
    fun logFeedLogged(feedType: String, amountMl: Double?) = log("feed_logged", bundle(
        "feed_type" to feedType,
        "has_amount" to (amountMl != null).toString()
    ))
    fun logDiaperLogged(diaperType: String) = log("diaper_logged", bundle("diaper_type" to diaperType))
    fun logSleepStarted() = log("sleep_started", null)
    fun logSleepEnded(durationMillis: Long) = log("sleep_ended", bundle("duration_minutes" to (durationMillis / 60_000).toString()))
    fun logWidgetAdded() = log("widget_added", null)
    fun logPaywallViewed(trigger: String) = log("paywall_viewed", bundle("trigger" to trigger))
    fun logPurchaseStarted(productId: String) = log("purchase_started", bundle("product_id" to productId))
    fun logPurchaseCompleted(productId: String) = log("purchase_completed", bundle("product_id" to productId))
    fun logPurchaseFailed(productId: String) = log("purchase_failed", bundle("product_id" to productId))
    fun logExportClicked() = log("export_clicked", null)
    fun logBackupEnabled() = log("backup_enabled", null)
    fun logLanguageChanged(language: String) = log("language_changed", bundle("language" to language))

    private fun log(event: String, params: Bundle?) {
        firebase.logEvent(event, params)
    }

    private fun bundle(vararg pairs: Pair<String, String>): Bundle = Bundle().apply {
        pairs.forEach { (key, value) -> putString(key, value) }
    }
}
