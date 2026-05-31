package com.nurtlina.app.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.nurtlina.app.data.remote.api.BackendApiService
import com.nurtlina.app.data.remote.api.EntitlementResponse
import com.nurtlina.app.data.remote.api.PurchaseVerificationRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

private val SUBS_PRODUCTS = setOf(
    EntitlementManager.PRODUCT_MONTHLY,
    EntitlementManager.PRODUCT_YEARLY,
)

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
    private val backendApiService: BackendApiService,
) : PurchasesUpdatedListener {

    private val _proStatus = MutableStateFlow(ProStatus.UNKNOWN)
    val proStatus: StateFlow<ProStatus> = _proStatus

    val isPro: Boolean get() = _proStatus.value != ProStatus.FREE && _proStatus.value != ProStatus.UNKNOWN

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    companion object {
        const val PRODUCT_MONTHLY = "nurtlina_pro_monthly"
        const val PRODUCT_YEARLY = "nurtlina_pro_yearly"
        const val PRODUCT_LIFETIME = "nurtlina_pro_lifetime"
        private val ENTITLEMENT_GRACE_PERIOD = Duration.ofDays(3)
    }

    init {
        scope.launch {
            applyCachedEntitlement(entitlementCacheRepository.get())
        }
    }

    fun connect() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryPurchases()
                }
            }
            override fun onBillingServiceDisconnected() {
                // Will retry on next launch
            }
        })
    }

    private fun queryPurchases() {
        val subsParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        billingClient.queryPurchasesAsync(subsParams) { _, purchases ->
            handlePurchases(purchases)
        }

        val inappParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billingClient.queryPurchasesAsync(inappParams) { _, purchases ->
            handlePurchases(purchases)
        }
    }

    private fun handlePurchases(purchases: List<Purchase>) {
        var hasPurchasedPro = false
        for (purchase in purchases) {
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                val status = when {
                    purchase.products.contains(PRODUCT_LIFETIME) -> ProStatus.LIFETIME
                    purchase.products.contains(PRODUCT_YEARLY) -> ProStatus.YEARLY
                    purchase.products.contains(PRODUCT_MONTHLY) -> ProStatus.MONTHLY
                    else -> null
                }
                if (status != null) {
                    hasPurchasedPro = true
                    _proStatus.value = status
                    cacheTemporaryUnlock(status)
                    verifyPurchaseWithBackend(purchase, status)
                }
                acknowledgePurchase(purchase)
            }
        }
        if (!hasPurchasedPro && _proStatus.value == ProStatus.UNKNOWN) {
            scope.launch {
                val cached = entitlementCacheRepository.get()
                if (!applyCachedEntitlement(cached)) {
                    _proStatus.value = ProStatus.FREE
                }
            }
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        if (!purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(params) { }
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            handlePurchases(purchases)
        }
    }

    fun restorePurchases() = queryPurchases()

    /**
     * Queries product details then immediately launches the Play Store purchase flow.
     * Must be called from a UI context where [activity] is visible.
     * Billing result is delivered via [onPurchasesUpdated].
     */
    fun launchBillingFlow(activity: Activity, productId: String) {
        val productType = if (productId in SUBS_PRODUCTS) {
            BillingClient.ProductType.SUBS
        } else {
            BillingClient.ProductType.INAPP
        }

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(productType)
                .build(),
        )
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { result, productDetailsList ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryProductDetailsAsync
            val productDetails = productDetailsList.firstOrNull() ?: return@queryProductDetailsAsync
            buildAndLaunchFlow(activity, productDetails, productType)
        }
    }

    private fun buildAndLaunchFlow(
        activity: Activity,
        productDetails: ProductDetails,
        productType: String,
    ) {
        val paramsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)

        if (productType == BillingClient.ProductType.SUBS) {
            val offerToken = productDetails.subscriptionOfferDetails
                ?.firstOrNull()
                ?.offerToken
                ?: return
            paramsBuilder.setOfferToken(offerToken)
        }

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(paramsBuilder.build()))
            .build()

        billingClient.launchBillingFlow(activity, flowParams)
    }

    private fun cacheTemporaryUnlock(status: ProStatus) {
        scope.launch {
            val now = Instant.now()
            entitlementCacheRepository.save(
                EntitlementCache(
                    isPro = true,
                    plan = status.name,
                    status = "TEMPORARY",
                    expiresAt = null,
                    lastVerifiedAt = null,
                    gracePeriodUntil = now.plus(ENTITLEMENT_GRACE_PERIOD),
                    source = "GOOGLE_PLAY_LOCAL",
                ),
            )
        }
    }

    private fun verifyPurchaseWithBackend(purchase: Purchase, status: ProStatus) {
        val productId = purchase.products.firstOrNull() ?: return
        scope.launch {
            runCatching {
                backendApiService.submitPurchase(
                    PurchaseVerificationRequest(
                        packageName = context.packageName,
                        productId = productId,
                        purchaseToken = purchase.purchaseToken,
                    ),
                )
            }.onSuccess { response ->
                entitlementCacheRepository.save(response.toCache(status))
                applyCachedEntitlement(entitlementCacheRepository.get())
            }
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

    private fun EntitlementResponse.toCache(fallbackStatus: ProStatus): EntitlementCache = EntitlementCache(
        isPro = isPro,
        plan = plan ?: fallbackStatus.name,
        status = status,
        expiresAt = expiresAt?.let { Instant.parse(it) },
        lastVerifiedAt = lastVerifiedAt?.let { Instant.parse(it) } ?: Instant.now(),
        gracePeriodUntil = gracePeriodUntil?.let { Instant.parse(it) },
        source = source ?: "BACKEND",
    )

    private fun String.toProStatus(): ProStatus = when (uppercase()) {
        "MONTHLY" -> ProStatus.MONTHLY
        "YEARLY" -> ProStatus.YEARLY
        "LIFETIME" -> ProStatus.LIFETIME
        else -> ProStatus.LIFETIME
    }
}
