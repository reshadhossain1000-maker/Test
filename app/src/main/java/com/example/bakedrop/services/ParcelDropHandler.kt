package com.example.bakedrop.services.parceldrop

import android.content.Context
import android.widget.Toast

object ParcelDropHandler {
    fun openService(context: Context) {
        Toast.makeText(context, "Parcel Drop Service Opening...", Toast.LENGTH_SHORT).show()
        // Parcel Drop delivery logic goes here
    }
}