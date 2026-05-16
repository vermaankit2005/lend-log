package com.lendlog.app.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import kotlinx.coroutines.*

class BillingManager(private val context: Context) {

    private var billingClient: BillingClient? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val PRODUCT_ID = "lendlog_unlimited"
    }

    fun connect() {
        billingClient = BillingClient.newBuilder(context)
            .setListener { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                    scope.launch { handlePurchases(purchases) }
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
                val activity = context as? Activity
                if (activity != null) {
                    val billingResult = client.launchBillingFlow(activity, flowParams)
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        onSuccess()
                    } else {
                        onFailure()
                    }
                } else {
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
        purchases.forEach { purchase ->
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                if (!purchase.isAcknowledged) {
                    val ackParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    billingClient?.acknowledgePurchase(ackParams)
                }
            }
        }
    }
}
