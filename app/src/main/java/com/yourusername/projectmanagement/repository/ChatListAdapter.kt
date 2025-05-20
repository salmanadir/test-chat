package com.yourusername.projectmanagement.repository

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.yourusername.projectmanagement.R
import com.yourusername.projectmanagement.models.ChatRoom
import com.yourusername.projectmanagement.models.User
import de.hdodenhof.circleimageview.CircleImageView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ChatListAdapter(
    private val onChatClicked: (ChatRoom) -> Unit
) : RecyclerView.Adapter<ChatListAdapter.ChatViewHolder>() {

    private val chatRooms = mutableListOf<ChatRoomWithUser>()

    data class ChatRoomWithUser(
        val chatRoom: ChatRoom,
        val otherUser: User,
        val unreadCount: Int = 0
    )

    fun updateChatRooms(newChatRooms: List<ChatRoomWithUser>) {
        chatRooms.clear()
        chatRooms.addAll(newChatRooms)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.chat_list_item, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(chatRooms[position])
    }

    override fun getItemCount() = chatRooms.size

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val profileImageView: CircleImageView = itemView.findViewById(R.id.profileImageView)
        private val usernameTextView: TextView = itemView.findViewById(R.id.usernameTextView)
        private val lastMessageTextView: TextView = itemView.findViewById(R.id.lastMessageTextView)
        private val timestampTextView: TextView = itemView.findViewById(R.id.timestampTextView)
        private val unreadBadge: TextView = itemView.findViewById(R.id.unreadBadge)

        fun bind(chatRoomWithUser: ChatRoomWithUser) {
            val (chatRoom, otherUser, unreadCount) = chatRoomWithUser

            usernameTextView.text = otherUser.username

            // Last message
            if (chatRoom.lastMessage.isNotEmpty()) {
                lastMessageTextView.text = chatRoom.lastMessage

                // Format timestamp
                val timestamp = Date(chatRoom.lastMessageTime)
                val calendar = Calendar.getInstance()
                val today = Calendar.getInstance()
                calendar.time = timestamp

                timestampTextView.text = when {
                    calendar.get(Calendar.YEAR) != today.get(Calendar.YEAR) -> {
                        SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(timestamp)
                    }

                    calendar.get(Calendar.DAY_OF_YEAR) != today.get(Calendar.DAY_OF_YEAR) -> {
                        SimpleDateFormat("MM/dd", Locale.getDefault()).format(timestamp)
                    }

                    else -> {
                        SimpleDateFormat("h:mm a", Locale.getDefault()).format(timestamp)
                    }
                }
            } else {
                lastMessageTextView.text = "No messages yet"
                timestampTextView.text = ""
            }

            // Load profile image
            if (otherUser.profilePictureUrl.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(otherUser.profilePictureUrl)
                    .placeholder(R.drawable.default_profile)
                    .into(profileImageView)
            } else {
                profileImageView.setImageResource(R.drawable.default_profile)
            }

            // Unread message count
            if (unreadCount > 0) {
                unreadBadge.visibility = View.VISIBLE
                unreadBadge.text = if (unreadCount > 99) "99+" else unreadCount.toString()
            } else {
                unreadBadge.visibility = View.GONE
            }

            // ✅ Set click listener
            itemView.setOnClickListener {
                onChatClicked(chatRoom)
            }
        }
    }
}