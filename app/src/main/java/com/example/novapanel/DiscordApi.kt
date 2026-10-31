package com.example.novapanel

import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

object DiscordApi {
    private val main = Handler(Looper.getMainLooper())

    private fun run(block: () -> Unit) = Thread(block).start()

    private fun post(cb: (Boolean, String) -> Unit, ok: Boolean, msg: String) {
        main.post { cb(ok, msg) }
    }

    fun sendWebhook(
        url: String, content: String, username: String, avatarUrl: String,
        embedTitle: String, embedDesc: String, color: Int, cb: (Boolean, String) -> Unit
    ) {
        if (url.isBlank()) { cb(false, "ضيف رابط Webhook أولاً"); return }
        run {
            try {
                val body = JSONObject()
                if (content.isNotBlank()) body.put("content", content)
                if (username.isNotBlank()) body.put("username", username)
                if (avatarUrl.isNotBlank()) body.put("avatar_url", avatarUrl)
                if (embedTitle.isNotBlank() || embedDesc.isNotBlank()) {
                    val embed = JSONObject()
                    if (embedTitle.isNotBlank()) embed.put("title", embedTitle)
                    if (embedDesc.isNotBlank()) embed.put("description", embedDesc)
                    embed.put("color", color and 0xFFFFFF)
                    body.put("embeds", JSONArray().put(embed))
                }
                val conn = URL(url).openConnection() as HttpsURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.outputStream.use { it.write(body.toString().toByteArray()) }
                val code = conn.responseCode
                if (code in 200..299) post(cb, true, "تم الإرسال ✅")
                else post(cb, false, "خطأ ($code)")
            } catch (e: Exception) {
                post(cb, false, "فشل: ${e.message}")
            }
        }
    }

    fun testBotToken(token: String, cb: (Boolean, String) -> Unit) {
        if (token.isBlank()) { cb(false, "ضيف توكن البوت أولاً"); return }
        run {
            try {
                val conn = URL("https://discord.com/api/v10/users/@me").openConnection() as HttpsURLConnection
                conn.setRequestProperty("Authorization", "Bot $token")
                val code = conn.responseCode
                if (code in 200..299) {
                    val res = JSONObject(conn.inputStream.bufferedReader().readText())
                    post(cb, true, "متصل كـ: ${res.optString("username")}")
                } else {
                    post(cb, false, "فشل التحقق ($code)")
                }
            } catch (e: Exception) {
                post(cb, false, "فشل: ${e.message}")
            }
        }
    }

    fun sendBotMessage(token: String, channelId: String, content: String, cb: (Boolean, String) -> Unit) {
        if (token.isBlank() || channelId.isBlank()) { cb(false, "ضيف التوكن ومعرّف القناة"); return }
        run {
            try {
                val body = JSONObject().put("content", content)
                val conn = URL("https://discord.com/api/v10/channels/$channelId/messages").openConnection() as HttpsURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Authorization", "Bot $token")
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.outputStream.use { it.write(body.toString().toByteArray()) }
                val code = conn.responseCode
                if (code in 200..299) post(cb, true, "تم الإرسال عبر البوت ✅")
                else post(cb, false, "خطأ ($code)")
            } catch (e: Exception) {
                post(cb, false, "فشل: ${e.message}")
            }
        }
    }
}
