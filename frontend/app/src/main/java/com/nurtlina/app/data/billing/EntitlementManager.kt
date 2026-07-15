package com.nurtlina.app.data.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.nurtlina.app.BuildConfig
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitPurchase
import com.revenuecat.purchases.awaitRestore
import com.revenuecat.purchases.interfaces.LogInCallback
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

enum class ProStatus {
    UNKNOWN,
    FREE,
    MONTHLY,
    YEARLY,
    LIFETIME,
}

@Singleton
class EntitlementManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val entitlementCacheRepository: EntitlementCacheRepository,
) {

    private val _proStatus = MutableStateFlow(ProStatus.UNKNOWN)
    val proStatus: StateFlow<ProStatus> = _proStatus

    val isPro: Boolean get() = _proStatus.value != ProStatus.FREE && _proStatus.value != ProStatus.UNKNOWN

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        const val PRODUCT_MONTHLY = "nurtlina_pro_monthly"
        const val PRODUCT_YEARLY = "nurtlina_pro_yearly"
        const val PRODUCT_LIFETIME = "nurtlina_pro_lifetime"
        const val ENTITLEMENT_PRO = "pro"

        private const val TAG = "EntitlementManager"
        private const val PACKAGE_MONTHLY = "\$rc_monthly"
        private const val PACKAGE_YEARLY = "\$rc_annual"
        private const val PACKAGE_LIFETIME = "\$rc_lifetime"
        private val ENTITLEMENT_GRACE_PERIOD = Duration.ofDays(3)
    }

    private val customerInfoListener = UpdatedCustomerInfoListener { customerInfo ->
        scope.launch {
            applyCustomerInfo(customerInfo)
        }
    }

    init {
        scope.launch {
            applyCachedEntitlement(entitlementCacheRepository.get())
        }
    }

    fun connect() {
        if (BuildConfig.REVENUECAT_API_KEY.isBlank()) {
            scope.launch {
                if (!applyCachedEntitlement(entitlementCacheRepository.get())) {
                    _proStatus.value = ProStatus.FREE
                }
            }
            Log.w(TAG, "RevenueCat API key is not configured.")
            return
        }

        val purchases = if (Purchases.isConfigured) {
            Purchases.sharedInstance
        } else {
            Purchases.configure(
                PurchasesConfiguration.Builder(context, BuildConfig.REVENUECAT_API_KEY)
                    .build(),
            )
        }
        purchases.updatedCustomerInfoListener = customerInfoListener
    }

    fun identify(appUserId: String) {
        connect()
        if (!Purchases.isConfigured || Purchases.sharedInstance.appUserID == appUserId) return

        Purchases.sharedInstance.logIn(
            appUserId,
            object : LogInCallback {
                override fun onReceived(customerInfo: CustomerInfo, created: Boolean) {
                    scope.launch { applyCustomerInfo(customerInfo) }
                }

                override fun onError(error: com.revenuecat.purchases.PurchasesError) {
                    Log.w(TAG, "RevenueCat login failed: ${error.message}")
                }
            },
        )
    }

    fun restorePurchases() {
        connect()
        if (!Purchases.isConfigured) return

        scope.launch {
            runCatching {
                Purchases.sharedInstance.awaitRestore()
            }.onSuccess { customerInfo ->
                applyCustomerInfo(customerInfo)
            }.onFailure { throwable ->
                Log.w(TAG, "RevenueCat restore failed.", throwable)
                if (!applyCachedEntitlement(entitlementCacheRepository.get())) {
                    _proStatus.value = ProStatus.FREE
                }
            }
        }
    }

    fun launchBillingFlow(activity: Activity, productId: String) {
        connect()
        if (!Purchases.isConfigured) return

        scope.launch {
            runCatching {
                val packageToPurchase = findPackageForProduct(productId) ?: return@runCatching null
                Purchases.sharedInstance.awaitPurchase(
                    PurchaseParams.Builder(activity, packageToPurchase).build(),
                ).customerInfo
            }.onSuccess { customerInfo ->
                if (customerInfo != null) {
                    applyCustomerInfo(customerInfo)
                } else {
                    Log.w(TAG, "RevenueCat package not found for $productId.")
                }
            }.onFailure { throwable ->
                Log.w(TAG, "RevenueCat purchase failed.", throwable)
            }
        }
    }

    /**
     * Toggles a process-local Pro override for hidden QA flows.
     *
     * This is deliberately unavailable in release builds and is never written to the entitlement
     * cache or backend, so it cannot be mistaken for a purchased entitlement.
     */
    fun toggleTestProStatus(): Boolean? {
        if (!BuildConfig.DEBUG) return null
        val enabled = !isPro
        _proStatus.value = if (enabled) ProStatus.LIFETIME else ProStatus.FREE
        Log.i(TAG, "Debug Pro override ${if (enabled) "enabled" else "disabled"}.")
        return enabled
    }

    private suspend fun findPackageForProduct(productId: String): Package? {
        val offering = Purchases.sharedInstance.awaitOfferings().current ?: return null
        val preferredPackageIdentifier = when (productId) {
            PRODUCT_MONTHLY -> PACKAGE_MONTHLY
            PRODUCT_YEARLY -> PACKAGE_YEARLY
            PRODUCT_LIFETIME -> PACKAGE_LIFETIME
            else -> null
        }
        return offering.availablePackages.firstOrNull { it.identifier == preferredPackageIdentifier }
            ?: offering.availablePackages.firstOrNull {
                it.product.id == productId || it.product.id.substringBefore(":") == productId
            }
    }

    private suspend fun applyCustomerInfo(customerInfo: CustomerInfo) {
        val entitlement = customerInfo.entitlements.active[ENTITLEMENT_PRO]
            ?: customerInfo.entitlements.active.values.firstOrNull()
        val status = entitlement?.productIdentifier?.toProStatus()
        val isPro = entitlement?.isActive == true && status != null
        val now = Instant.now()
        val cache = EntitlementCache(
            isPro = isPro,
            plan = status?.name,
            status = if (isPro) "ACTIVE" else "FREE",
            expiresAt = entitlement?.expirationDate?.toInstantCompat(),
            lastVerifiedAt = now,
            gracePeriodUntil = if (isPro) now.plus(ENTITLEMENT_GRACE_PERIOD) else null,
            source = "REVENUECAT",
        )
        entitlementCacheRepository.save(cache)
        if (!applyCachedEntitlement(cache)) {
            _proStatus.value = ProStatus.FREE
        }
    }

    private fun applyCachedEntitlement(cache: EntitlementCache): Boolean {
        val now = Instant.now()
        val withinGrace = cache.gracePeriodUntil?.isAfter(now) == true
        val notExpired = cache.expiresAt?.isAfter(now) ?: true
        if (cache.isPro && (notExpired || withinGrace)) {
            _proStatus.value = cache.plan?.toProStatus() ?: ProStatus.LIFETIME
            return true
        }
        return false
    }

    private fun Date.toInstantCompat(): Instant = Instant.ofEpochMilli(time)

    private fun String.toProStatus(): ProStatus = when {
        contains(PRODUCT_MONTHLY, ignoreCase = true) || contains("monthly", ignoreCase = true) -> ProStatus.MONTHLY
        contains(PRODUCT_YEARLY, ignoreCase = true) ||
            contains("yearly", ignoreCase = true) ||
            contains("annual", ignoreCase = true) -> ProStatus.YEARLY
        contains(PRODUCT_LIFETIME, ignoreCase = true) || contains("lifetime", ignoreCase = true) -> ProStatus.LIFETIME
        uppercase() == "MONTHLY" -> ProStatus.MONTHLY
        uppercase() == "YEARLY" -> ProStatus.YEARLY
        uppercase() == "LIFETIME" -> ProStatus.LIFETIME
        else -> ProStatus.LIFETIME
    }
}
