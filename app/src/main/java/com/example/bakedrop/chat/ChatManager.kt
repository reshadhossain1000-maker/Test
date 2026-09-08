package com.example.bakedrop.chat

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object ChatManager {

    // 🔑 আপনার দেওয়া আসল DeepSeek API Key
    private const val DEEPSEEK_API_KEY = "sk-fa0fa02c39c94d819fb91d96457b4d32"
    private const val DEEPSEEK_ENDPOINT = "https://api.deepseek.com/chat/completions"

    // 🌟 Master System Prompt (Strict Language & Brevity Rules, No Emojis)
    private const val SYSTEM_PROMPT = """
        You are BakeDrop's official Live Customer Support Assistant for bakery merchants and riders in Dhaka, Bangladesh.

        STRICT RULES:
        1. IF the user asks in Bengali -> Reply strictly in natural Bengali script (বাংলা ভাষায়).
        2. IF the user asks in English -> Reply strictly in professional English.
        3. IF the user writes in Banglish (Bengali in English alphabet, e.g., "kemon acho", "rate koto", "cake delivery") -> ALWAYS reply in proper Bengali script (বাংলা হরফে). Never reply in Banglish.
        4. IF mixed Bengali + English -> Reply in Bengali.
        5. Keep answers VERY SHORT, CRISP, AND PRECISE (Maximum 1 to 2 sentences). No long paragraphs.
        6. ABSOLUTELY NO EMOJIS in your output under any circumstances.

        BAKEDROP BUSINESS DATA:
        - Cake Rider Express: 120 BDT (30-45 minutes delivery).
        - Parcel Drop: 80 BDT for dry bakery parcels.
        - Mid Night Surprise Delivery: 200 BDT (11 PM to 2 AM).
        - Bake Shop: Wholesale marketplace for bakery items.
        - Need a Helping Hand?: On-demand store and kitchen assistant support.
        - 100% Damage Recovery Policy: Full compensation provided if items are damaged in transit.
    """

    suspend fun fetchDeepSeekReply(userMessage: String): String = withContext(Dispatchers.IO) {
        try {
            val url = URL(DEEPSEEK_ENDPOINT)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $DEEPSEEK_API_KEY")
            conn.connectTimeout = 12000
            conn.readTimeout = 12000
            conn.doOutput = true

            val payload = JSONObject().apply {
                put("model", "deepseek-chat")
                val messagesArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", SYSTEM_PROMPT)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", userMessage)
                    })
                }
                put("messages", messagesArray)
                put("temperature", 0.6)
                put("max_tokens", 150)
            }

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                reader.close()

                val jsonResponse = JSONObject(response)
                val choices = jsonResponse.getJSONArray("choices")
                if (choices.length() > 0) {
                    val messageObj = choices.getJSONObject(0).getJSONObject("message")
                    val rawReply = messageObj.getString("content").trim()
                    // Remove any accidental emojis
                    return@withContext rawReply.replace(Regex("[\\p{So}\\p{Cn}]"), "")
                }
            }
            return@withContext getSmartFallback(userMessage)
        } catch (e: Exception) {
            return@withContext getSmartFallback(userMessage)
        }
    }

    private fun getSmartFallback(userMessage: String): String {
        val msg = userMessage.trim().lowercase()
        return when {
            msg.contains("চার্জ") || msg.contains("rate") || msg.contains("ভাড়া") || msg.contains("koto") || msg.contains("cost") ->
                "BakeDrop এক্সপ্রেস কেক রাইডার ১২০ টাকা, পার্সেল ড্রপ ৮০ টাকা এবং মিড নাইট ডেলিভারি ২০০ টাকা।"
            msg.contains("নষ্ট") || msg.contains("damage") || msg.contains("ক্ষতি") || msg.contains("nosto") ->
                "ডেলিভারির সময় কোনো ক্ষতি হলে আমাদের ১০০% ড্যামেজ রিকভারি গ্যারান্টির আওতায় ক্ষতিপূরণ পাবেন।"
            msg.contains("সময়") || msg.contains("time") || msg.contains("somoy") ->
                "আমাদের এক্সপ্রেস কেক রাইডার ৩০ থেকে ৪৫ মিনিটের মধ্যে ডেলিভারি সম্পন্ন করে।"
            else ->
                "ধন্যবাদ আপনার বার্তার জন্য। BakeDrop লাইভ সাপোর্ট আপনাকে সর্বাত্মক সহায়তা করতে প্রস্তুত।"
        }
    }
}