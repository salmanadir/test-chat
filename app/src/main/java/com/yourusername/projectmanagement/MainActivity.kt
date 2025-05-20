package com.yourusername.projectmanagement

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
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

            navController.addOnDestinationChangedListener { controller, destination, _ ->
                // Naviguer une seule fois si on démarre avec une notification
                intent.getStringExtra("chatId")?.let { chatId ->
                    intent.removeExtra("chatId")  // Pour éviter de répéter la navigation
                    val bundle = Bundle().apply {
                        putString("chatId", chatId)
                    }
                    if (destination.id != R.id.chatConversationFragment) {
                        controller.navigate(R.id.chatConversationFragment, bundle)
                    }
                }
            }
            FirebaseAuth.getInstance()
                .signInWithEmailAndPassword("test@gmail.com", "password") // a changer avec email dans la base de donnee authentication
                .addOnSuccessListener {
                    Log.d("Auth", "Connexion automatique réussie")
                    updateFcmToken()
                }
                .addOnFailureListener {
                    Log.e("Auth", "Échec de connexion", it)
                }

        }


    }

    private fun updateFcmToken() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.e("FCM", "Utilisateur non connecté, token non mis à jour")
            return
        }

        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUser.uid)
                .update("fcmToken", token)
                .addOnSuccessListener {
                    Log.d("FCM", "Token FCM mis à jour")
                }
                .addOnFailureListener {
                    Log.e("FCM", "Échec de mise à jour du token", it)
                }
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
            .addOnFailureListener { e ->
                Log.e("UserStatus", "Failed to update last seen", e)
            }
    }
}