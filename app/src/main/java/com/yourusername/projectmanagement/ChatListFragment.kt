package com.yourusername.projectmanagement

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.yourusername.projectmanagement.models.ChatRoom
import com.yourusername.projectmanagement.models.User
import com.yourusername.projectmanagement.repository.ChatListAdapter
import com.yourusername.projectmanagement.repository.ChatRepository

class ChatListFragment : Fragment() {
    private var _binding: FragmentChatListBinding? = null
    private val binding get() = _binding!!

    private val chatRepository = ChatRepository()
    private val chatListAdapter = ChatListAdapter { chatRoom ->
        navigateToChatConversation(chatRoom)
    }

    private val lifecycleScope = androidx.lifecycle.lifecycleScope

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup RecyclerView
        binding.chatRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = chatListAdapter
        }

        // Setup FAB for new chat
        binding.newChatFab.setOnClickListener {
            navigateToUserSelection()
        }

        // Load chat rooms
        loadChatRooms()
    }

    private fun loadChatRooms() {
        lifecycleScope.launch {
            chatRepository.getUserChatRooms().collect { chatRooms ->
                val chatRoomsWithUsers = chatRooms.map { chatRoom ->
                    // Get the other user in the chat
                    val otherUserId = chatRoom.participants.firstOrNull { it != FirebaseAuth.getInstance().currentUser?.uid }
                    val otherUser = otherUserId?.let { chatRepository.getUserById(it) } ?: User()

                    // Get unread message count
                    val unreadCount = chatRepository.getUnreadMessageCount(chatRoom.chatId)

                    ChatListAdapter.ChatRoomWithUser(chatRoom, otherUser, unreadCount)
                }

                chatListAdapter.updateChatRooms(chatRoomsWithUsers)

                // Show/hide empty state
                binding.emptyStateTextView.visibility =
                    if (chatRoomsWithUsers.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun navigateToChatConversation(chatRoom: ChatRoom) {
        val action = ChatListFragmentDirections.actionChatListFragmentToChatConversationFragment(chatRoom.chatId)
        findNavController().navigate(action)
    }

    private fun navigateToUserSelection() {
        val action = ChatListFragmentDirections.actionChatListFragmentToUserSelectionFragment()
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}