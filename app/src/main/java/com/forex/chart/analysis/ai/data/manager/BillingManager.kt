package com.forex.chart.analysis.ai.data.manager

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class BillingManager(private val context: Context) {

    private val _isProUser = MutableStateFlow(false)
    val isProUser = _isProUser.asStateFlow()

    // ✅ NEW: Track whether Google Play Store check has completed
    private val _isBillingClientReady = MutableStateFlow(false)
    val isBillingClientReady = _isBillingClientReady.asStateFlow()

    private val _productPrices = MutableStateFlow<Map<String, String>>(emptyMap())
    val productPrices = _productPrices.asStateFlow()

    private val TAG = "BillingManager"

    val weeklyPlanId = "weekly_pro_plan"
    val monthlyPlanId = "monthly_pro_plan"
    val yearlyPlanId = "yearly_pro_plan"

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.d(TAG, "User canceled the purchase.")
        } else {
            Log.e(TAG, "Purchase failed. Code: ${billingResult.responseCode}")
        }
    }

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases()
        .build()

    init {
        startConnection()
    }

    private fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing Setup Finished")
                    checkSubscriptionStatus()
                    loadProductPrices()

                    // ✅ Checking complete, safe to navigate
                    _isBillingClientReady.value = true
                } else {
                    Log.e(TAG, "Billing Setup Failed: ${billingResult.debugMessage}")
                    _isBillingClientReady.value = true // Prevent app from hanging even if check fails
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.e(TAG, "Billing Service Disconnected. Retrying...")
                Handler(Looper.getMainLooper()).postDelayed({
                    startConnection()
                }, 2000)
            }
        })
    }

    fun refreshPrices() {
        if (billingClient.isReady) {
            loadProductPrices()
        } else {
            startConnection()
        }
    }

    private fun loadProductPrices() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder().setProductId(weeklyPlanId).setProductType(BillingClient.ProductType.SUBS).build(),
            QueryProductDetailsParams.Product.newBuilder().setProductId(monthlyPlanId).setProductType(BillingClient.ProductType.SUBS).build(),
            QueryProductDetailsParams.Product.newBuilder().setProductId(yearlyPlanId).setProductType(BillingClient.ProductType.SUBS).build()
        )

        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val newPrices = mutableMapOf<String, String>()
                productDetailsList.forEach { product ->
                    val offer = product.subscriptionOfferDetails?.firstOrNull()
                    val price = offer?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
                    if (price != null) {
                        newPrices[product.productId] = price
                    }
                }
                _productPrices.value = newPrices
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        _isProUser.value = true
                    }
                }
            } else {
                _isProUser.value = true
            }
        }
    }

    private fun checkSubscriptionStatus() {
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                _isProUser.value = purchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            }
        }
    }

    fun purchaseWeeklyPlan(activity: Activity) { launchPurchaseFlow(activity, weeklyPlanId) }
    fun purchaseMonthlyPlan(activity: Activity) { launchPurchaseFlow(activity, monthlyPlanId) }
    fun purchaseYearlyPlan(activity: Activity) { launchPurchaseFlow(activity, yearlyPlanId) }

    private fun launchPurchaseFlow(activity: Activity, planId: String) {
        if (!billingClient.isReady) {
            startConnection()
            return
        }
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder().setProductId(planId).setProductType(BillingClient.ProductType.SUBS).build()
        )
        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                val productDetails = productDetailsList[0]
                val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: return@queryProductDetailsAsync
                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(productDetails).setOfferToken(offerToken).build()))
                    .build()
                activity.runOnUiThread {
                    billingClient.launchBillingFlow(activity, billingFlowParams)
                }
            }
        }
    }
}