package com.thuanpx.utils.billing

import com.google.gson.annotations.SerializedName

/**
 * Created by Luan on 5/30/2020.
 */

data class OptionPoint(
    @SerializedName("sku") var sku: String?,
    @SerializedName("point") var point: Int?,
    @SerializedName("price") var price: Int?
)