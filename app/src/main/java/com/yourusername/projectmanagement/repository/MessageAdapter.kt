package com.yourusername.projectmanagement.repository

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.yourusername.projectmanagement.R
import com.yourusername.projectmanagement.models.Message
import com.yourusername.projectmanagement.models.User
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    private val messages = mutableListOf<MessageWithUser>()
    private var currentUserId: String = ""

    data class MessageWithUser(
        val message: Message,
        val user: User? = null
    )

    fun updateMessages(newMessages: List<MessageWithUser>, userId: String) {
        messages.clear()
        messages.addAll(newMessages)
        currentUserId = userId
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages[position].message
        return if (message.senderId == currentUserId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SENT) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.message_item_sent, parent, false)
            SentMessageViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.message_item_received, parent, false)
            ReceivedMessageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val messageWithUser = messages[position]

        when (holder) {
            is SentMessageViewHolder -> holder.bind(messageWithUser)
            is ReceivedMessageViewHolder -> holder.bind(messageWithUser)
        }
    }

    override fun getItemCount() = messages.size

    inner class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageTextView: TextView = itemView.findViewById(R.id.messageTextView)
        private val timeTextView: TextView = itemView.findViewById(R.id.timeTextView)
        private val readStatusImageView: ImageView = itemView.findViewById(R.id.readStatusImageView)

        fun bind(messageWithUser: MessageWithUser) {
            val message = messageWithUser.message

            messageTextView.text = message.content ?: ""


            // Format time
            val timestamp = message.timestamp
            if (timestamp != 0L) {
                val date = Date(timestamp)
                timeTextView.text = SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
            } else {
                timeTextView.text = ""
            }


            // Set read status
            readStatusImageView.setImageResource(
                if (message.isRead) R.drawable.ic_message_read
                else R.drawable.ic_message_delivered
            )
        }
    }

    inner class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageTextView: TextView = itemView.findViewById(R.id.messageTextView)
        private val timeTextView: TextView = itemView.findViewById(R.id.timeTextView)

        fun bind(messageWithUser: MessageWithUser) {
            val message = messageWithUser.message

            messageTextView.text = message.content ?: ""


            // Format time
            val timestamp = message.timestamp
            if (timestamp > 0L) {
                try {
                    val date = Date(timestamp)
                    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                    timeTextView.text = timeFormat.format(date)
                } catch (e: Exception) {
                    timeTextView.text = "--:--"
                    e.printStackTrace()
                }
            } else {
                timeTextView.text = ""
            }

        }
    }
}