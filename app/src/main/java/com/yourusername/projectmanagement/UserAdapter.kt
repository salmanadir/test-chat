package com.yourusername.projectmanagement

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.yourusername.projectmanagement.models.User
import de.hdodenhof.circleimageview.CircleImageView

class UserAdapter(
    private val onUserClicked: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    private val users = mutableListOf<User>()

    fun updateUsers(newUsers: List<User>) {
        users.clear()
        users.addAll(newUsers)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.user_list_item, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount() = users.size

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val profileImageView: CircleImageView = itemView.findViewById(R.id.profileImageView)
        private val usernameTextView: TextView = itemView.findViewById(R.id.usernameTextView)

        fun bind(user: User) {
            usernameTextView.text = user.username

            // Load profile image
            if (user.profilePictureUrl.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(user.profilePictureUrl)
                    .placeholder(R.drawable.default_profile)
                    .into(profileImageView)
            } else {
                profileImageView.setImageResource(R.drawable.default_profile)
            }

            // Set click listener
            itemView.setOnClickListener {
                onUserClicked(user)
            }
        }
    }
}