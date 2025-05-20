
package com.yourusername.projectmanagement

import android.os.Bundle
import android.provider.Settings.Global.putString
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Handle notification click
        intent.getStringExtra("chatId")?.let { chatId ->
            // Navigate to chat conversation
            val navController = findNavController(R.id.nav_host_fragment)
            val bundle = Bundle().apply {
                putString("chatId", chatId)
            }
            navController.navigate(R.id.chatConversationFragment, bundle)
        }

        // Initialize FCM token
        updateFcmToken()
    }

    private fun updateFcmToken() {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            val currentUser = FirebaseAuth.getInstance().currentUser ?: return@addOnSuccessListener
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUser.uid)
                .update("fcmToken", token)
        }
    }

    override fun onResume() {
        super.onResume()
        updateUserLastSeen()
    }

    private fun updateUserLastSeen() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val lastSeen = System.currentTimeMillis()

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(currentUser.uid)
            .update("lastSeen", lastSeen)
    }
}
