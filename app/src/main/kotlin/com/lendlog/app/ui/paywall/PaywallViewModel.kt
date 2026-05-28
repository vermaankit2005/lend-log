package com.lendlog.app.ui.paywall

import android.app.Activity
import androidx.lifecycle.ViewModel
import com.lendlog.app.billing.BillingManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PaywallViewModel @Inject constructor(
    private val billingManager: BillingManager
) : ViewModel() {

    fun launchBillingFlow(
        activity: Activity,
        onSuccess: () -> Unit,
        onFailure: () -> Unit,
        onPending: () -> Unit
    ) = billingManager.launchBillingFlow(activity, onSuccess, onFailure, onPending)

    fun restorePurchases(onRestored: () -> Unit, onFailure: () -> Unit) =
        billingManager.restorePurchases(onRestored, onFailure)
}
