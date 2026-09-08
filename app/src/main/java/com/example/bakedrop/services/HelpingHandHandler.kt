package com.example.bakedrop.services.helpinghand

import android.content.Context
import android.widget.Toast

object HelpingHandHandler {
    fun openService(context: Context) {
        Toast.makeText(context, "Need a Helping Hand Assistant Opening...", Toast.LENGTH_SHORT).show()
        // Helping Hand assistant request logic goes here
    }
}