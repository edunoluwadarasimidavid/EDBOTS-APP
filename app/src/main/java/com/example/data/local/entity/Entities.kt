package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bots")
data class BotEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phoneNumber: String?,
    val state: String,
    val uptimeSeconds: Long,
    val messagesProcessed: Long,
    val activeChats: Int,
    val batteryLevel: Int,
    val isCharging: Boolean,
    val lastSeenTimestamp: Long
)

@Entity(tableName = "commands")
data class CommandEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val category: String,
    val enabled: Boolean,
    val adminOnly: Boolean
)

@Entity(tableName = "auto_replies")
data class AutoReplyEntity(
    @PrimaryKey val id: String,
    val trigger: String,
    val response: String,
    val matchType: String,
    val enabled: Boolean
)
