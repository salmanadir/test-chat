package com.yourusername.projectmanagement

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.yourusername.projectmanagement.models.User
import com.yourusername.projectmanagement.repository.ChatRepository

class UserSelectionFragment : Fragment() {
    private var _binding: FragmentUserSelectionBinding? = null
    private val binding get() = _binding!!

    private val chatRepository = ChatRepository()
    private val userAdapter = UserAdapter { user ->
        createChatWithUser(user)
    }

    private val lifecycleScope = androidx.lifecycle.lifecycleScope

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup toolbar navigation
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        // Setup RecyclerView
        binding.usersRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = userAdapter
        }

        // Load users
        loadUsers()
    }

    private fun loadUsers() {
        lifecycleScope.launch {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch

            // This is a simplified approach - you might want to implement pagination or filtering
            val usersRef = FirebaseFirestore.getInstance().collection("users")
            val users = usersRef.get().await().documents
                .mapNotNull { doc ->
                    doc.toObject(User::class.java)?.copy(userId = doc.id)
                }
                .filter { it.userId != currentUserId }

            userAdapter.updateUsers(users)

            // Show/hide empty state
            binding.emptyStateTextView.visibility =
                if (users.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun createChatWithUser(user: User) {
        lifecycleScope.launch {
            try {
                val result = chatRepository.createChatRoom(user.userId)

                if (result.isSuccess) {
                    val chatId = result.getOrThrow()
                    // Navigate to chat conversation
                    val action = UserSelectionFragmentDirections
                        .actionUserSelectionFragmentToChatConversationFragment(chatId)
                    findNavController().navigate(action)
                } else {
                    Toast.makeText(requireContext(), "Failed to create chat", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}