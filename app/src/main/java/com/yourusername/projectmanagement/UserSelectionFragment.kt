package com.yourusername.projectmanagement

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.yourusername.projectmanagement.databinding.FragmentUserSelectionBinding
import com.yourusername.projectmanagement.models.User
import com.yourusername.projectmanagement.repository.ChatRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UserSelectionFragment : Fragment() {
    private var _binding: FragmentUserSelectionBinding? = null
    private val binding get() = _binding!!

    private val chatRepository = ChatRepository()
    private val userAdapter = UserAdapter { user ->
        createChatWithUser(user)
    }

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
        navigateToUserSelection()

    }
    private fun navigateToUserSelection() {
        Log.d("ChatListFragment", "Attempting to navigate to UserSelectionFragment")
        try {
            val action = ChatListFragmentDirections.actionChatListFragmentToUserSelectionFragment()
            findNavController().navigate(action)
            Log.d("ChatListFragment", "Navigation successful")
        } catch (e: Exception) {
            Log.e("ChatListFragment", "Navigation failed", e)
            // Handle the error
        }
    }

    private fun loadUsers() {
        lifecycleScope.launch {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch

            val usersRef = FirebaseFirestore.getInstance().collection("users")
            val users = usersRef.get().await().documents
                .mapNotNull { doc ->
                    doc.toObject(User::class.java)?.copy(userId = doc.id)
                }
                .filter { it.userId != currentUserId }

            userAdapter.updateUsers(users)

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
