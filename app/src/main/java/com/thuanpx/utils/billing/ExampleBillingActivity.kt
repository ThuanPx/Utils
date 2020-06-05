package com.thuanpx.utils.billing

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.Observer
import com.android.billingclient.api.BillingFlowParams
import com.thuanpx.androidinapppurchase.billing.BillingClientLifecycle
import com.thuanpx.utils.R
import kotlinx.android.synthetic.main.activity_example_billing.*

class ExampleBillingActivity : AppCompatActivity() {

    private val billingClientLifecycle: BillingClientLifecycle
        get() = BillingClientLifecycle.getInstance(application)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example_billing)
        initInapp()

        buyPoint.setOnClickListener {
            buyPoint()
        }
    }

    private fun initInapp() {
        lifecycle.addObserver(billingClientLifecycle)
        billingClientLifecycle.purchaseSuccess.observe(this, Observer {
            Log.d(ExampleBillingActivity::class.java.simpleName, "initInapp: $it")

        })
        billingClientLifecycle.purchaseError.observe(this, Observer {
            Log.d(ExampleBillingActivity::class.java.simpleName, "initInapp: $it")
        })
    }

    private fun buyPoint() {
        val skusWithSkuDetails = billingClientLifecycle.skusWithSkuDetails

        val skuDetails = skusWithSkuDetails.value?.get(BillingConstant.listOptionPoint[0].sku) ?: run {
            return
        }

        val billingBuilder = BillingFlowParams.newBuilder().setSkuDetails(skuDetails)
        billingClientLifecycle.launchBillingFlow(this, billingBuilder.build())
    }
}