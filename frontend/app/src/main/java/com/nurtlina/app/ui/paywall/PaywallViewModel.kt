package com.nurtlina.app.ui.paywall

import android.app.Activity
import androidx.lifecycle.ViewModel
import com.nurtlina.app.data.billing.EntitlementManager
import com.nurtlina.app.data.billing.ProStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class PaywallViewModel @Inject constructor(
    private val entitlementManager: EntitlementManager,
) : ViewModel() {

    /** Live Pro status — updates immediately after a successful purchase. */
    val proStatus: StateFlow<ProStatus> = entitlementManager.proStatus

    /**
     * Launches the Google Play billing flow for the selected plan.
     * [activity] must be the currently visible foreground Activity.
     * The result is delivered asynchronously via [proStatus].
     */
    fun subscribe(activity: Activity, plan: String) {
        val productId = when (plan) {
            "monthly" -> EntitlementManager.PRODUCT_MONTHLY
            "yearly" -> EntitlementManager.PRODUCT_YEARLY
            "lifetime" -> EntitlementManager.PRODUCT_LIFETIME
            else -> return
        }
        entitlementManager.launchBillingFlow(activity, productId)
    }

    /** Re-queries the Play Store to restore previously purchased entitlements. */
    fun restorePurchases() {
        entitlementManager.restorePurchases()
    }
}
