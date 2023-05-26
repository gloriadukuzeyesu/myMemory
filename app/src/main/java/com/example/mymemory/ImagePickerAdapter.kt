package com.example.mymemory

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.mymemory.models.BoardSize
import kotlin.math.min

class ImagePickerAdapter(
    private val context: Context,
    private val imageUris: List<Uri>,
    private val boardSize: BoardSize,
    private val imageClickListener: ImageClickListener

) : RecyclerView.Adapter<ImagePickerAdapter.ViewHolder>() {

    // will be called when user clicked on the grey image wanting to upload a pic
    interface ImageClickListener  {
        fun onPlaceHolderClicked()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.card_image, parent, false)
        val cardWidth = parent.width / boardSize.getWidth()
        val cardHeight = parent.width / boardSize.getHeight()
        val cardSizeLength = min(cardHeight, cardWidth)
        val layoutParams = view.findViewById<ImageView>(R.id.ivCustomImage).layoutParams
        layoutParams.width = cardSizeLength
        layoutParams.height = cardSizeLength
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // how to display the Uis
        if (position < imageUris.size) { // means the user has picked the image therefore display it
            holder.bind(imageUris[position])
        } else {
            holder.bind() // means the user has not yet picked the image. But they want to choose one. ( myNotes: Jya muyindi intent  kugira bahitemo image)
        }

    }

    override fun getItemCount(): Int {
        return boardSize.getNumPairs()
    }

    inner class ViewHolder (itemView:View) : RecyclerView.ViewHolder(itemView) {
        // get the reference to the image View
        private val ivCustomImage = itemView.findViewById<ImageView>(R.id.ivCustomImage)

        fun bind(uri: Uri) {
            ivCustomImage.setImageURI(uri) // binds the the image to that position
            ivCustomImage.setOnClickListener(null) // disable the click listener
        }

        fun bind() {
            ivCustomImage.setOnClickListener{
                // means user wants to choose an image.
                // the goal here is to launch an intent for the user to select photos.
                imageClickListener.onPlaceHolderClicked()
            }


        }

    }

}


