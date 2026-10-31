package com.example.novapanel

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object Prefs {
    private const val KEY = "servers"

    fun load(ctx: Context): MutableList<Server> {
        val raw = ctx.getSharedPreferences("np", Context.MODE_PRIVATE).getString(KEY, "[]") ?: "[]"
        val arr = JSONArray(raw)
        val list = mutableListOf<Server>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                Server(
                    id = o.optString("id"),
                    name = o.optString("name"),
                    webhookUrl = o.optString("webhookUrl"),
                    botToken = o.optString("botToken"),
                    channelId = o.optString("channelId"),
                    color = o.optInt("color", 0xFF5865F2.toInt())
                )
            )
        }
        return list
    }

    fun save(ctx: Context, servers: List<Server>) {
        val arr = JSONArray()
        for (s in servers) {
            val o = JSONObject()
            o.put("id", s.id)
            o.put("name", s.name)
            o.put("webhookUrl", s.webhookUrl)
            o.put("botToken", s.botToken)
            o.put("channelId", s.channelId)
            o.put("color", s.color)
            arr.put(o)
        }
        ctx.getSharedPreferences("np", Context.MODE_PRIVATE).edit().putString(KEY, arr.toString()).apply()
    }

    fun newId(): String = UUID.randomUUID().toString().take(8)
}
