package com.lendlog.app.billing

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.android.billingclient.api.*
import kotlinx.coroutines.*

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

class BillingManager(private val context: Context) {

    private var billingClient: BillingClient? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var purchaseSuccessCallback: (() -> Unit)? = null
    private var purchaseFailureCallback: (() -> Unit)? = null

    companion object {
        const val PRODUCT_ID = "lendlog_unlimited"
    }

    fun connect() {
        billingClient = BillingClient.newBuilder(context)
            .setListener { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                    scope.launch { handlePurchases(purchases) }
                } else if (billingResult.responseCode != BillingClient.BillingResponseCode.USER_CANCELED) {
                    val cb = purchaseFailureCallback
                    purchaseSuccessCallback = null
                    purchaseFailureCallback = null
                    scope.launch { withContext(Dispatchers.Main) { cb?.invoke() } }
                }
            }
            .enablePendingPurchases()
            .build()

        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {}
            override fun onBillingServiceDisconnected() {}
        })
    }

    fun disconnect() {
        billingClient?.endConnection()
        scope.cancel()
    }

    fun launchBillingFlow(onSuccess: () -> Unit, onFailure: () -> Unit) {
        val client = billingClient ?: run { onFailure(); return }
        purchaseSuccessCallback = onSuccess
        purchaseFailureCallback = onFailure

        scope.launch {
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(
                    listOf(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(PRODUCT_ID)
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build()
                    )
                ).build()

            val result = client.queryProductDetails(params)
            val productDetails = result.productDetailsList?.firstOrNull()
            if (productDetails == null) {
                purchaseSuccessCallback = null
                purchaseFailureCallback = null
                withContext(Dispatchers.Main) { onFailure() }
                return@launch
            }

            val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()

            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productDetailsParams))
                .build()

            withContext(Dispatchers.Main) {
                val activity = context.findActivity()
                if (activity != null) {
                    val billingResult = client.launchBillingFlow(activity, flowParams)
                    if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                        purchaseSuccessCallback = null
                        purchaseFailureCallback = null
                        onFailure()
                    }
                    // Success delivered via PurchasesUpdatedListener
                } else {
                    purchaseSuccessCallback = null
                    purchaseFailureCallback = null
                    onFailure()
                }
            }
        }
    }

    fun restorePurchases(onRestored: () -> Unit, onFailure: () -> Unit) {
        val client = billingClient ?: run { onFailure(); return }

        scope.launch {
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()

            val result = client.queryPurchasesAsync(params)
            val hasPurchase = result.purchasesList.any { purchase ->
                purchase.products.contains(PRODUCT_ID) &&
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED
            }

            withContext(Dispatchers.Main) {
                if (hasPurchase) onRestored() else onFailure()
            }
        }
    }

    private suspend fun handlePurchases(purchases: List<Purchase>) {
        var anyPurchased = false
        purchases.forEach { purchase ->
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                anyPurchased = true
                if (!purchase.isAcknowledged) {
                    val ackParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    billingClient?.acknowledgePurchase(ackParams)
                }
            }
        }
        val successCb = purchaseSuccessCallback
        val failureCb = purchaseFailureCallback
        purchaseSuccessCallback = null
        purchaseFailureCallback = null
        withContext(Dispatchers.Main) {
            if (anyPurchased) successCb?.invoke() else failureCb?.invoke()
        }
    }
}
