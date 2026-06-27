package com.crow.tradewolf.ui

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.crow.tradewolf.R
import com.google.android.material.button.MaterialButton
import java.net.URL
import kotlin.concurrent.thread

data class MarketplacePostItem(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val price: String = "",
    val imageUrl: String = "",
    val userId: String = "",
    val userName: String = ""
)

class MarketplacePostAdapter(
    private val onContactClick: (MarketplacePostItem) -> Unit,
    private val onDetailClick: (MarketplacePostItem) -> Unit
) : RecyclerView.Adapter<MarketplacePostAdapter.PostViewHolder>() {

    private val posts = mutableListOf<MarketplacePostItem>()
    private val filteredPosts = mutableListOf<MarketplacePostItem>()

    fun setPosts(newPosts: List<MarketplacePostItem>) {
        posts.clear()
        posts.addAll(newPosts)

        filteredPosts.clear()
        filteredPosts.addAll(newPosts)

        notifyDataSetChanged()
    }

    fun filter(query: String) {
        val text = query.trim().lowercase()

        filteredPosts.clear()

        if (text.isEmpty()) {
            filteredPosts.addAll(posts)
        } else {
            filteredPosts.addAll(
                posts.filter {
                    it.title.lowercase().contains(text) ||
                            it.description.lowercase().contains(text) ||
                            it.category.lowercase().contains(text) ||
                            it.price.lowercase().contains(text) ||
                            it.userName.lowercase().contains(text)
                }
            )
        }

        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_marketplace_post, parent, false)

        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(filteredPosts[position])
    }

    override fun getItemCount(): Int = filteredPosts.size

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val ivPostImage = itemView.findViewById<ImageView>(R.id.ivPostImage)
        private val tvPostTitle = itemView.findViewById<TextView>(R.id.tvPostTitle)
        private val tvPostInfo = itemView.findViewById<TextView>(R.id.tvPostInfo)
        private val btnContactPost = itemView.findViewById<MaterialButton>(R.id.btnContactPost)
        private val btnDetailPost = itemView.findViewById<MaterialButton>(R.id.btnDetailPost)

        fun bind(post: MarketplacePostItem) {
            tvPostTitle.text = post.title.ifBlank { "Publicación sin nombre" }

            val info = buildString {
                if (post.category.isNotBlank()) append(post.category)
                if (post.price.isNotBlank()) {
                    if (isNotBlank()) append("  •  ")
                    append("S/ ${post.price}")
                }
            }

            tvPostInfo.text = info.ifBlank { "Sin categoría / Sin precio" }

            loadImage(post.imageUrl, ivPostImage)

            btnContactPost.setOnClickListener {
                onContactClick(post)
            }

            btnDetailPost.setOnClickListener {
                onDetailClick(post)
            }
        }

        private fun loadImage(imageUrl: String, imageView: ImageView) {
            imageView.setImageResource(android.R.drawable.ic_menu_gallery)

            if (imageUrl.isBlank()) return

            thread {
                try {
                    val url = URL(imageUrl)
                    val bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream())

                    imageView.post {
                        imageView.setImageBitmap(bitmap)
                    }
                } catch (e: Exception) {
                    imageView.post {
                        imageView.setImageResource(android.R.drawable.ic_menu_gallery)
                    }
                }
            }
        }
    }
}