package com.example.bakedrop.status

import android.content.Context
import android.widget.Toast

object StatusManager {
    // ভবিষ্যতে রিয়েলটাইম অর্ডার ট্র্যাকিং ও ডেলিভারি হিস্ট্রির লজিক এখানে থাকবে
    fun refreshDeliveryStatus(context: Context) {
        Toast.makeText(context, "Delivery status refreshed", Toast.LENGTH_SHORT).show()
    }

    fun getSampleActiveOrdersCount(): Int = 2
    fun getSampleCompletedOrdersCount(): Int = 1186
}