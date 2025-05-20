package com.yourusername.projectmanagement.models

data class ChatRoom(
    val chatId: String = "",
    val participants: List<String> = listOf(),
    val lastMessage: String = "",
    val lastMessageTime: Long = 0,
    val lastSenderId: String = "",
    val projectId: String? = null
) {
    // Empty constructor for Firestore
    constructor() : this("", listOf(), "", 0, "", null)
}