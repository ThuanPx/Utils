package com.thuanpx.utils

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.thuanpx.ktext.context.startActivity
import com.thuanpx.utils.billing.ExampleBillingActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btExampleBilling.setOnClickListener {
            startActivity(ExampleBillingActivity::class)
        }
    }
}