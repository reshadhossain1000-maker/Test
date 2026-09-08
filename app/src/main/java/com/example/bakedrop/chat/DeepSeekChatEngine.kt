package com.example.bakedrop.chat

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object DeepSeekChatEngine {

    // 🔑 আপনার আসল DeepSeek API Key
    const val API_KEY = "sk-fa0fa02c39c94d819fb91d96457b4d32"
    private const val ENDPOINT = "https://api.deepseek.com/chat/completions"
    private const val MODEL = "deepseek-chat"

    // 🌟 Claude-এর তৈরি কড়া মাল্টি-লিঙ্গুয়াল সিস্টেম প্রম্পট (ইমোজি নিষিদ্ধ)
    private val SYSTEM_PROMPT = """
        You are BakeDrop's live chat assistant for a premium bakery & cake delivery merchant platform in Dhaka, Bangladesh.

        STRICT LANGUAGE RULES:
        - If the user writes in Bangla script, reply only in Bangla script.
        - If the user writes in English, reply only in English.
        - If the user writes Banglish (Bengali words in English letters, e.g., "kemon acho", "rate koto"), reply strictly in proper Bengali script (বাংলা হরফে) — never reply in Banglish.
        - Keep answers VERY SHORT, DIRECT, AND CONCISE (1-2 sentences maximum).
        - Absolutely NO EMOJIS, emoticons, or decorative symbols of any kind in any language.

        BAKEDROP BUSINESS RULES:
        - Cake Rider Express: 120 BDT (30-45 mins delivery).
        - Parcel Drop: 80 BDT for dry bakery parcels.
        - Mid Night Surprise Delivery: 200 BDT (11 PM to 2 AM).
        - Bake Shop: Wholesale hub for bakeries.
        - Need a Helping Hand?: On-demand store and kitchen assistant.
        - 100% Damage Recovery Policy for transit damages.
    """.trimIndent()

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val JSON = "application/json; charset=utf-8".toMediaType()

    suspend fun fetchReply(userMessage: String): String = withContext(Dispatchers.IO) {
        try {
            val messagesArray = JSONArray().apply {
                put(JSONObject().put("role", "system").put("content", SYSTEM_PROMPT))
                put(JSONObject().put("role", "user").put("content", userMessage))
            }

            val payload = JSONObject().apply {
                put("model", MODEL)
                put("messages", messagesArray)
                put("temperature", 0.4)
                put("max_tokens", 150)
            }

            val request = Request.Builder()
                .url(ENDPOINT)
                .addHeader("Authorization", "Bearer $API_KEY")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(JSON))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext getOfflineFallback(userMessage)
                }
                val json = JSONObject(bodyString)
                val rawReply = json.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")

                stripEmojis(rawReply).trim()
            }
        } catch (e: Exception) {
            getOfflineFallback(userMessage)
        }
    }

    private fun stripEmojis(text: String): String {
        val emojiRegex = Regex(
            "[\\u203C-\\u3299\\uD83C\\uD000-\\uDFFF\\uD83D\\uD000-\\uDFFF\\uD83E\\uD000-\\uDFFF\\u2600-\\u27BF\\uFE0F]"
        )
        return emojiRegex.replace(text, "").replace(Regex(" {2,}"), " ")
    }

    private fun getOfflineFallback(userMessage: String): String {
        val msg = userMessage.trim().lowercase()
        return when {
            msg.contains("চার্জ") || msg.contains("rate") || msg.contains("ভাড়া") || msg.contains("koto") ->
                "BakeDrop এক্সপ্রেস কেক রাইডার ১২০ টাকা, পার্সেল ড্রপ ৮০ টাকা এবং এসি ফ্রিজিং ভ্যান ২৫০ টাকা।"
            msg.contains("নষ্ট") || msg.contains("damage") || msg.contains("ক্ষতি") ->
                "ডেলিভারির সময় কোনো ক্ষতি হলে আমাদের ১০০% ড্যামেজ রিকভারি পলিসির আওতায় পূর্ণ ক্ষতিপূরণ দেওয়া হয়।"
            else ->
                "ধন্যবাদ আপনার বার্তার জন্য। BakeDrop লাইভ সাপোর্ট আপনাকে সর্বাত্মক সহায়তা করতে প্রস্তুত।"
        }
    }
}