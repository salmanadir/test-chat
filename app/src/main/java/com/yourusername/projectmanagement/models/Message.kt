package com.yourusername.projectmanagement.models

data class Message(
    val messageId: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val content: String = "",
    val timestamp: Long = 0,
    val isRead: Boolean = false,
    val attachmentUrl: String? = null
) {
    // Empty constructor for Firestore
    constructor() : this("", "", "", "", 0, false, null)
}