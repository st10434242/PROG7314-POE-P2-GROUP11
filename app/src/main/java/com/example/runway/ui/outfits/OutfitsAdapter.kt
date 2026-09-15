package com.example.runway.ui.outfits

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.runway.R
import com.example.runway.databinding.ItemOutfitBinding
import com.example.runway.domain.model.Outfit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// One row per saved outfit (IIE, 2026; Android Open Source Project, 2020a).
// The thumbnail is the try-on render, decoded off the main thread.

class OutfitsAdapter(
    private val scope: CoroutineScope,
    private val onClick: (Outfit) -> Unit,
) : ListAdapter<Outfit, OutfitsAdapter.OutfitViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OutfitViewHolder {
        val binding = ItemOutfitBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OutfitViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OutfitViewHolder, position: Int) =
        holder.bind(getItem(position))

    override fun onViewRecycled(holder: OutfitViewHolder) {
        super.onViewRecycled(holder)
        holder.cancel()
    }

    inner class OutfitViewHolder(
        private val binding: ItemOutfitBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        private var loadJob: Job? = null

        fun bind(outfit: Outfit) {
            binding.root.setOnClickListener { onClick(outfit) }
            binding.outfitName.text = outfit.name
            binding.outfitCount.text = binding.root.context.resources.getQuantityString(
                R.plurals.rw_outfit_garment_count, outfit.garmentCount, outfit.garmentCount
            )

            // Says plainly when an outfit has no picture, rather than showing a gap.
            binding.outfitNoRender.isVisible = outfit.renderPath.isNullOrBlank()
            load(outfit.renderPath)
        }

        private fun load(path: String?) {
            cancel()
            binding.outfitImage.setImageDrawable(null)
            if (path.isNullOrBlank()) return

            loadJob = scope.launch {
                val bitmap = withContext(Dispatchers.IO) { decode(path) } ?: return@launch
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    binding.outfitImage.setImageBitmap(bitmap)
                }
            }
        }

        fun cancel() {
            loadJob?.cancel()
            loadJob = null
        }

        private fun decode(path: String): Bitmap? {
            val file = File(path)
            if (!file.exists()) return null

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            if (bounds.outWidth <= 0) return null

            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > THUMBNAIL_PX) sample *= 2
            return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
        }
    }

    private companion object {
        const val THUMBNAIL_PX = 240

        val DIFF = object : DiffUtil.ItemCallback<Outfit>() {
            override fun areItemsTheSame(old: Outfit, new: Outfit) = old.id == new.id

            override fun areContentsTheSame(old: Outfit, new: Outfit) = old == new
        }
    }
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
Android Open Source Project, 2020a. Create dynamic lists with RecyclerView. [online] Available at: <https://developer.android.com/develop/ui/views/layout/recyclerview> [Accessed 15 September 2026].
*/
