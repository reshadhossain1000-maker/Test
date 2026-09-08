package com.example.bakedrop.services.midnight

import android.content.Context
import android.widget.Toast

object MidnightHandler {
    fun openService(context: Context) {
        Toast.makeText(context, "Mid Night Surprise Delivery Opening...", Toast.LENGTH_SHORT).show()
        // Midnight booking logic goes here
    }
}