package com.thuanpx.utils.billing


/**
 * Created by Luan on 5/30/2020.
 */

object BillingConstant {

    const val KEY_PURCHASE_1 = "android.test.purchased"
    const val KEY_PURCHASE_2 = "android.test.canceled"
    const val KEY_PURCHASE_3 = "android.test.item_unavailable"

    val listSku = listOf(KEY_PURCHASE_1, KEY_PURCHASE_2, KEY_PURCHASE_3)

    val listOptionPoint = listOf(
        OptionPoint(KEY_PURCHASE_1, 100, 100),
        OptionPoint(KEY_PURCHASE_1, 200, 200),
        OptionPoint(KEY_PURCHASE_1, 300, 300)
    )
}