package com.yourusername.projectmanagement.repository

import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.yourusername.projectmanagement.models.ChatRoom
import com.yourusername.projectmanagement.models.Message
import com.yourusername.projectmanagement.models.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ChatRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val currentUserId: String
        get() = auth.currentUser?.uid ?: throw IllegalStateException("User must be logged in")

    // Get all chat rooms for the current user
    fun getUserChatRooms(): Flow<List<ChatRoom>> = callbackFlow {
        val listener = firestore.collection("chatRooms")
            .whereArrayContains("participants", currentUserId)
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val chatRooms = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ChatRoom::class.java)?.copy(chatId = doc.id)
                } ?: emptyList()

                trySend(chatRooms)
            }

        awaitClose { listener.remove() }
    }

    // Get messages for a specific chat room
    fun getChatMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        val listener = firestore.collection("messages")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Message::class.java)?.copy(messageId = doc.id)
                } ?: emptyList()

                trySend(messages)
            }

        awaitClose { listener.remove() }
    }

    // Get user details by ID
    suspend fun getUserById(userId: String): User? {
        return try {
            val userDoc = firestore.collection("users").document(userId).get().await()
            userDoc.toObject(User::class.java)?.copy(userId = userDoc.id)
        } catch (e: Exception) {
            null
        }
    }

    // Send a new message
    suspend fun sendMessage(chatId: String, content: String, attachmentUri: Uri? = null): Result<Message> {
        return try {
            // Upload attachment if exists
            val attachmentUrl = attachmentUri?.let { uri ->
                val ref = storage.reference.child("chat_attachments/$chatId/${UUID.randomUUID()}")
                ref.putFile(uri).await()
                ref.downloadUrl.await().toString()
            }

            val messageId = UUID.randomUUID().toString()
            val timestamp = System.currentTimeMillis()

            val message = Message(
                messageId = messageId,
                chatId = chatId,
                senderId = currentUserId,
                content = content,
                timestamp = timestamp,
                isRead = false,
                attachmentUrl = attachmentUrl
            )

            // Add message to messages collection
            firestore.collection("messages")
                .document(chatId)
                .collection("messages")
                .document(messageId)
                .set(message)
                .await()

            // Update last message in chat room
            firestore.collection("chatRooms")
                .document(chatId)
                .update(
                    mapOf(
                        "lastMessage" to content,
                        "lastMessageTime" to timestamp,
                        "lastSenderId" to currentUserId
                    )
                ).await()

            Result.success(message)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Create a new chat room
    suspend fun createChatRoom(participantId: String, projectId: String? = null): Result<String> {
        return try {
            val participants = listOf(currentUserId, participantId)

            // Check if chat room already exists
            val existingChatQuery = firestore.collection("chatRooms")
                .whereArrayContainsAny("participants", participants)
                .get()
                .await()

            val existingChatRoom = existingChatQuery.documents.find { doc ->
                val chatParticipants = doc.get("participants") as? List<*>
                chatParticipants?.containsAll(participants) == true && chatParticipants.size == participants.size
            }

            if (existingChatRoom != null) {
                return Result.success(existingChatRoom.id)
            }

            // Create new chat room
            val chatRoom = ChatRoom(
                participants = participants,
                lastMessage = "",
                lastMessageTime = System.currentTimeMillis(),
                lastSenderId = currentUserId,
                projectId = projectId
            )

            val chatRoomRef = firestore.collection("chatRooms").document()
            chatRoomRef.set(chatRoom).await()

            Result.success(chatRoomRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Mark messages as read
    suspend fun markMessagesAsRead(chatId: String) {
        try {
            val unreadMessagesQuery = firestore.collection("messages")
                .document(chatId)
                .collection("messages")
                .whereEqualTo("isRead", false)
                .whereNotEqualTo("senderId", currentUserId)
                .get()
                .await()

            val batch = firestore.batch()

            unreadMessagesQuery.documents.forEach { doc ->
                batch.update(doc.reference, "isRead", true)
            }

            batch.commit().await()
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error marking messages as read", e)
        }
    }

    // Get unread message count for a chat
    suspend fun getUnreadMessageCount(chatId: String): Int {
        return try {
            val query = firestore.collection("messages")
                .document(chatId)
                .collection("messages")
                .whereEqualTo("isRead", false)
                .whereNotEqualTo("senderId", currentUserId)
                .count()
                .get(AggregateSource.SERVER)
                .await()

            query.count.toInt()
        } catch (e: Exception) {
            0
        }
    }
}