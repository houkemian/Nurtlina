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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
) : PurchasesUpdatedListener {

    private val _proStatus = MutableStateFlow(ProStatus.UNKNOWN)
    val proStatus: StateFlow<ProStatus> = _proStatus

    val isPro: Boolean get() = _proStatus.value != ProStatus.FREE && _proStatus.value != ProStatus.UNKNOWN

    private var billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    companion object {
        const val PRODUCT_MONTHLY = "nurtlina_pro_monthly"
        const val PRODUCT_YEARLY = "nurtlina_pro_yearly"
        const val PRODUCT_LIFETIME = "nurtlina_pro_lifetime"
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
        for (purchase in purchases) {
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                when {
                    purchase.products.contains(PRODUCT_LIFETIME) -> _proStatus.value = ProStatus.LIFETIME
                    purchase.products.contains(PRODUCT_YEARLY) -> _proStatus.value = ProStatus.YEARLY
                    purchase.products.contains(PRODUCT_MONTHLY) && _proStatus.value == ProStatus.FREE ->
                        _proStatus.value = ProStatus.MONTHLY
                }
                acknowledgePurchase(purchase)
            }
        }
        if (_proStatus.value == ProStatus.UNKNOWN) {
            _proStatus.value = ProStatus.FREE
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
}
