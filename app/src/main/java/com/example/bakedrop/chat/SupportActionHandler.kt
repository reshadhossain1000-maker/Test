package com.example.bakedrop.chat

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.widget.*
import com.example.bakedrop.R
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object SupportActionHandler {

    private const val SUPPORT_EMAIL = "support@bakedrop.com"
    private const val SUPPORT_HOTLINE = "+8801700000000" // ← আপনার হটলাইন নাম্বার

    // 🌟 প্লাস (+) বাটনে চাপ দিলে ৩টি অপশনের আধুনিক বটম শিট ওপেন হবে
    fun showSupportActionSheet(context: Context) {
        val dialog = BottomSheetDialog(context)
        val view = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0F172A")) // Deep Navy
            setPadding(40, 30, 40, 50)
        }

        val title = TextView(context).apply {
            text = "BakeDrop Support Desk Options"
            setTextColor(Color.WHITE)
            textSize = 16f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 30)
        }
        view.addView(title)

        // Option 1: 📝 অভিযোগ দায়ের করুন
        view.addView(createActionItem(context, "📝  Report an Issue / Complaint", "#38BDF8") {
            dialog.dismiss()
            openComplaintPortal(context)
        })

        // Option 2: ✉️ সরাসরি ইমেইল পাঠান
        view.addView(createActionItem(context, "✉️  Send Official Email Support", "#FFFFFF") {
            dialog.dismiss()
            openEmailClient(context)
        })

        // Option 3: 📞 জরুরি হটলাইন কল
        view.addView(createActionItem(context, "📞  Call Emergency Hotline", "#10B981") {
            dialog.dismiss()
            openPhoneDialer(context)
        })

        dialog.setContentView(view)
        dialog.show()
    }

    private fun createActionItem(context: Context, textStr: String, textColorHex: String, onClick: () -> Unit): LinearLayout {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 24, 0, 24)
            isClickable = true
            isFocusable = true
            setBackgroundResource(android.R.drawable.list_selector_background)
            setOnClickListener { onClick() }
        }
        val tv = TextView(context).apply {
            text = textStr
            setTextColor(Color.parseColor(textColorHex))
            textSize = 14.5f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        row.addView(tv)
        return row
    }

    // 🌟 ১. সরাসরি ইমেইল ক্লায়েন্ট চালু করা
    private fun openEmailClient(context: Context) {
        val user = FirebaseAuth.getInstance().currentUser
        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$SUPPORT_EMAIL")
            putExtra(Intent.EXTRA_SUBJECT, "BakeDrop Merchant Support Request - [UID: ${user?.uid ?: "N/A"}]")
            putExtra(Intent.EXTRA_TEXT, "Hello BakeDrop Support Team,\n\nI need assistance with:\n")
        }
        try {
            context.startActivity(Intent.createChooser(emailIntent, "Send Email via..."))
        } catch (e: Exception) {
            Toast.makeText(context, "No Email app found on your phone", Toast.LENGTH_SHORT).show()
        }
    }

    // 🌟 ২. সরাসরি জরুরি কল ডায়াল প্যাড চালু করা
    private fun openPhoneDialer(context: Context) {
        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$SUPPORT_HOTLINE")
        }
        try {
            context.startActivity(dialIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to open phone dialer", Toast.LENGTH_SHORT).show()
        }
    }

    // 🌟 ৩. অভিযোগ দায়ের পোর্টাল (Firestore-এ লাইফটাইম সেভ হবে)
    private fun openComplaintPortal(context: Context) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 20)
        }

        val etSubject = EditText(context).apply {
            hint = "Complaint Subject (e.g. Rider Delay / Cake Damage)"
            textSize = 14f
        }
        val etDetails = EditText(context).apply {
            hint = "Describe your issue in detail..."
            textSize = 14f
            minLines = 3
        }

        layout.addView(etSubject)
        layout.addView(etDetails)

        AlertDialog.Builder(context)
            .setTitle("Report an Issue to Admin")
            .setView(layout)
            .setPositiveButton("Submit") { _, _ ->
                val subject = etSubject.text.toString().trim()
                val details = etDetails.text.toString().trim()

                if (subject.isNotEmpty() && details.isNotEmpty()) {
                    val complaintData = hashMapOf(
                        "merchantUid" to user.uid,
                        "merchantEmail" to (user.email ?: ""),
                        "subject" to subject,
                        "details" to details,
                        "status" to "Pending Admin Review",
                        "timestamp" to System.currentTimeMillis()
                    )

                    db.collection("complaints").add(complaintData)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Complaint submitted successfully! Admin will review shortly.", Toast.LENGTH_LONG).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Failed to submit. Please try again.", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}