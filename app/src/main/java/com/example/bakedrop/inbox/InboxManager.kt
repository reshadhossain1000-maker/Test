package com.example.bakedrop.inbox

import android.content.Context
import android.widget.Toast

object InboxManager {
    // ভবিষ্যতে পুশ নোটিফিকেশন, পেআউট অ্যালার্ট ও অফার মেসেজের লজিক এখানে থাকবে
    fun loadNotifications(context: Context) {
        Toast.makeText(context, "Notifications updated", Toast.LENGTH_SHORT).show()
    }

    fun getUnreadCount(): Int = 3
}