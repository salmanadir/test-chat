package com.yourusername.projectmanagement

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.yourusername.projectmanagement.databinding.FragmentChatConversationBinding
import com.yourusername.projectmanagement.models.User
import com.yourusername.projectmanagement.repository.ChatRepository
import com.yourusername.projectmanagement.repository.MessageAdapter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ChatConversationFragment : Fragment() {
    private var _binding: FragmentChatConversationBinding? = null
    private val binding get() = _binding!!

    private val chatRepository = ChatRepository()
    private val messageAdapter = MessageAdapter()

    private val args: ChatConversationFragmentArgs by navArgs()
    private val chatId: String by lazy { args.chatId }

    private var otherUser: User? = null
    private var selectedAttachmentUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatConversationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup RecyclerView
        binding.messagesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
            }
            adapter = messageAdapter
        }

        // Setup toolbar navigation
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        // Load chat info and messages
        loadChatInfo()
        loadMessages()

        // Mark messages as read when opening the chat
        lifecycleScope.launch {
            chatRepository.markMessagesAsRead(chatId)
        }

        // Setup send button
        binding.sendButton.setOnClickListener {
            sendMessage()
        }

        // Setup attachment button
        binding.attachmentButton.setOnClickListener {
            openAttachmentPicker()
        }
    }

    private fun loadChatInfo() {
        lifecycleScope.launch {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch

            chatRepository.getUserChatRooms().collect { chatRooms ->
                val chatRoom = chatRooms.firstOrNull { it.chatId == chatId } ?: return@collect

                val otherUserId = chatRoom.participants.firstOrNull { it != currentUserId } ?: return@collect
                otherUser = chatRepository.getUserById(otherUserId)

                otherUser?.let { user ->
                    binding.usernameTextView.text = user.username

                    if (user.profilePictureUrl.isNotEmpty()) {
                        Glide.with(requireContext())
                            .load(user.profilePictureUrl)
                            .placeholder(R.drawable.default_profile)
                            .into(binding.profileImageView)
                    }

                    val lastSeen = user.lastSeen
                    val now = System.currentTimeMillis()
                    val isOnline = now - lastSeen < 2 * 60 * 1000 // 2 minutes

                    binding.statusTextView.text = if (isOnline) {
                        "Online"
                    } else {
                        val lastSeenDate = Date(lastSeen)
                        val calendar = Calendar.getInstance()
                        val today = Calendar.getInstance()
                        calendar.time = lastSeenDate

                        when {
                            calendar.get(Calendar.YEAR) != today.get(Calendar.YEAR) -> {
                                "Last seen " + SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(lastSeenDate)
                            }
                            calendar.get(Calendar.DAY_OF_YEAR) != today.get(Calendar.DAY_OF_YEAR) -> {
                                "Last seen " + SimpleDateFormat("MM/dd", Locale.getDefault()).format(lastSeenDate)
                            }
                            else -> {
                                "Last seen " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(lastSeenDate)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun loadMessages() {
        lifecycleScope.launch {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch

            chatRepository.getChatMessages(chatId).collect { messages ->
                val messagesWithUsers = messages.map { message ->
                    MessageAdapter.MessageWithUser(message)
                }

                messageAdapter.updateMessages(messagesWithUsers, currentUserId)

                if (messages.isNotEmpty()) {
                    binding.messagesRecyclerView.scrollToPosition(messages.size - 1)
                }
            }
        }
    }

    private fun sendMessage() {
        val content = binding.messageEditText.text.toString().trim()

        if (content.isNotEmpty() || selectedAttachmentUri != null) {
            lifecycleScope.launch {
                binding.messageEditText.isEnabled = false
                binding.sendButton.isEnabled = false

                try {
                    val result = chatRepository.sendMessage(chatId, content, selectedAttachmentUri)

                    if (result.isSuccess) {
                        binding.messageEditText.text.clear()
                        selectedAttachmentUri = null
                    } else {
                        Toast.makeText(requireContext(), "Failed to send message", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    binding.messageEditText.isEnabled = true
                    binding.sendButton.isEnabled = true
                }
            }
        }
    }

    private fun openAttachmentPicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                "image/*",
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            ))
        }
        startActivityForResult(intent, REQUEST_CODE_PICK_ATTACHMENT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_PICK_ATTACHMENT && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                selectedAttachmentUri = uri
                Toast.makeText(requireContext(), "Attachment selected", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val REQUEST_CODE_PICK_ATTACHMENT = 1001
    }
}
