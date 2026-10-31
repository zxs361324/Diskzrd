package com.example.novapanel

data class Server(
    var id: String,
    var name: String,
    var webhookUrl: String = "",
    var botToken: String = "",
    var channelId: String = "",
    var color: Int = 0xFF5865F2.toInt()
)
