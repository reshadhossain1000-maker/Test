package com.example.bakedrop.ui.assurance

import android.content.Context
import android.widget.Toast

object AssuranceHandler {
    fun onDamageRecoveryClicked(context: Context) {
        Toast.makeText(context, "100% Damage Recovery Policy Details Opening...", Toast.LENGTH_SHORT).show()
    }

    fun onInstantPayoutClicked(context: Context) {
        Toast.makeText(context, "Instant Payout Guarantee Details Opening...", Toast.LENGTH_SHORT).show()
    }
}