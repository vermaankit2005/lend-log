package com.lendlog.app.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.lendlog.app.data.datastore.AppPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appPreferences: AppPreferences
) {
    private enum class ConnectionState { DISCONNECTED, CONNECTING, CONNECTED }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    private var billingClient: BillingClient? = null
    private var reconnectAttempt = 0
    private var intentionalDisconnect = false

    private var purchaseSuccessCallback: (() -> Unit)? = null
    private var purchaseFailureCallback: (() -> Unit)? = null
    private var purchasePendingCallback: (() -> Unit)? = null

    // Exposed so the UI can show a "payment pending" banner across sessions.
    val hasPendingPurchase = MutableStateFlow(false)

    companion object {
        const val PRODUCT_ID = "lendlog_unlimited"
        private const val MAX_RECONNECT_ATTEMPTS = 5
    }

    fun connect() {
        if (_connectionState.value != ConnectionState.DISCONNECTED) return
        intentionalDisconnect = false
        _connectionState.value = ConnectionState.CONNECTING

        billingClient = BillingClient.newBuilder(context)
            .setListener { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                    scope.launch { handlePurchases(purchases) }
                } else if (billingResult.responseCode != BillingClient.BillingResponseCode.USER_CANCELED) {
                    val cb = purchaseFailureCallback
                    clearCallbacks()
                    scope.launch { withContext(Dispatchers.Main) { cb?.invoke() } }
                }
            }
            .enablePendingPurchases()
            .build()

        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    reconnectAttempt = 0
                    _connectionState.value = ConnectionState.CONNECTED
                    // Reconcile any purchases that arrived while the app was offline.
                    scope.launch { reconcilePurchases() }
                } else {
                    _connectionState.value = ConnectionState.DISCONNECTED
                    scheduleReconnect()
                }
            }

            override fun onBillingServiceDisconnected() {
                _connectionState.value = ConnectionState.DISCONNECTED
                scheduleReconnect()
            }
        })
    }

    fun disconnect() {
        intentionalDisconnect = true
        billingClient?.endConnection()
        _connectionState.value = ConnectionState.DISCONNECTED
        // Do NOT cancel scope — in-flight acknowledgements must complete to avoid auto-refund.
    }

    private fun scheduleReconnect() {
        if (intentionalDisconnect || reconnectAttempt >= MAX_RECONNECT_ATTEMPTS) return
        val delayMs = (1L shl reconnectAttempt) * 1000L // 1s, 2s, 4s, 8s, 16s
        reconnectAttempt++
        scope.launch {
            delay(delayMs)
            _connectionState.value = ConnectionState.DISCONNECTED
            connect()
        }
    }

    private suspend fun awaitConnected(timeoutMs: Long = 10_000L): Boolean =
        withTimeoutOrNull(timeoutMs) {
            _connectionState.first { it == ConnectionState.CONNECTED }
            true
        } ?: false

    fun launchBillingFlow(
        activity: Activity,
        onSuccess: () -> Unit,
        onFailure: () -> Unit,
        onPending: () -> Unit = {}
    ) {
        purchaseSuccessCallback = onSuccess
        purchaseFailureCallback = onFailure
        purchasePendingCallback = onPending

        scope.launch {
            if (!awaitConnected()) {
                clearCallbacks()
                withContext(Dispatchers.Main) { onFailure() }
                return@launch
            }
            val client = billingClient ?: run {
                clearCallbacks()
                withContext(Dispatchers.Main) { onFailure() }
                return@launch
            }

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
                clearCallbacks()
                withContext(Dispatchers.Main) { onFailure() }
                return@launch
            }

            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(productDetails)
                            .build()
                    )
                ).build()

            withContext(Dispatchers.Main) {
                val billingResult = client.launchBillingFlow(activity, flowParams)
                if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                    clearCallbacks()
                    onFailure()
                }
                // Successful purchase delivered via PurchasesUpdatedListener.
            }
        }
    }

    fun restorePurchases(onRestored: () -> Unit, onFailure: () -> Unit) {
        scope.launch {
            if (!awaitConnected()) {
                withContext(Dispatchers.Main) { onFailure() }
                return@launch
            }
            val client = billingClient ?: run {
                withContext(Dispatchers.Main) { onFailure() }
                return@launch
            }

            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()

            val purchases = client.queryPurchasesAsync(params).purchasesList
            val validPurchases = purchases.filter { purchase ->
                purchase.products.contains(PRODUCT_ID) &&
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED
            }

            if (validPurchases.isEmpty()) {
                withContext(Dispatchers.Main) { onFailure() }
                return@launch
            }

            var anyUnlocked = false
            validPurchases.forEach { purchase ->
                val acked = if (!purchase.isAcknowledged) {
                    acknowledgeWithRetry(client, purchase.purchaseToken)
                } else {
                    true
                }
                if (acked) anyUnlocked = true
            }

            if (anyUnlocked) {
                appPreferences.setUnlocked(true)
                withContext(Dispatchers.Main) { onRestored() }
            } else {
                withContext(Dispatchers.Main) { onFailure() }
            }
        }
    }

    // Called on every successful connection to catch purchases made while offline
    // (e.g. pending → purchased transition, or a purchase on another device).
    private suspend fun reconcilePurchases() {
        val client = billingClient ?: return
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val purchases = client.queryPurchasesAsync(params).purchasesList

        val purchased = purchases.filter { purchase ->
            purchase.products.contains(PRODUCT_ID) &&
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED
        }
        purchased.forEach { purchase ->
            if (!purchase.isAcknowledged) {
                acknowledgeWithRetry(client, purchase.purchaseToken)
            }
        }
        if (purchased.isNotEmpty()) {
            appPreferences.setUnlocked(true)
            hasPendingPurchase.value = false
        }
    }

    private suspend fun handlePurchases(purchases: List<Purchase>) {
        val client = billingClient ?: return
        var anyPurchased = false
        var anyPending = false

        purchases.forEach { purchase ->
            when (purchase.purchaseState) {
                Purchase.PurchaseState.PURCHASED -> {
                    val acked = if (!purchase.isAcknowledged) {
                        acknowledgeWithRetry(client, purchase.purchaseToken)
                    } else {
                        true
                    }
                    if (acked) {
                        appPreferences.setUnlocked(true)
                        anyPurchased = true
                    }
                }
                Purchase.PurchaseState.PENDING -> {
                    // Do not acknowledge — Google will upgrade this to PURCHASED once payment clears.
                    anyPending = true
                    hasPendingPurchase.value = true
                }
                else -> { /* UNSPECIFIED state — ignore */ }
            }
        }

        val successCb = purchaseSuccessCallback
        val failureCb = purchaseFailureCallback
        val pendingCb = purchasePendingCallback
        clearCallbacks()

        withContext(Dispatchers.Main) {
            when {
                anyPurchased -> successCb?.invoke()
                anyPending   -> pendingCb?.invoke()
                else         -> failureCb?.invoke()
            }
        }
    }

    private suspend fun acknowledgeWithRetry(
        client: BillingClient,
        purchaseToken: String,
        maxAttempts: Int = 3
    ): Boolean {
        val ackParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()
        repeat(maxAttempts) { attempt ->
            val result = client.acknowledgePurchase(ackParams)
            if (result.responseCode == BillingClient.BillingResponseCode.OK) return true
            if (attempt < maxAttempts - 1) delay((1L shl attempt) * 1000L)
        }
        return false
    }

    private fun clearCallbacks() {
        purchaseSuccessCallback = null
        purchaseFailureCallback = null
        purchasePendingCallback = null
    }
}
