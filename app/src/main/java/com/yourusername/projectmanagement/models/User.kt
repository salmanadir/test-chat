package com.yourusername.projectmanagement.models

data class User(
    val userId: String = "",
    val username: String = "",
    val email: String = "",
    val profilePictureUrl: String = "",
    val lastSeen: Long = 0
) {
    // Empty constructor for Firestore
    constructor() : this("", "", "", "", 0)
}