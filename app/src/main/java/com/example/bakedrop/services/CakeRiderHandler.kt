package com.example.bakedrop.services.cakerider

import android.content.Context
import android.widget.Toast

object CakeRiderHandler {
    fun openService(context: Context) {
        Toast.makeText(context, "Cake Rider Service Opening...", Toast.LENGTH_SHORT).show()
        // Cake Rider algorithm & booking logic goes here
    }
}