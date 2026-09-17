package com.example.runway.ui.wardrobe

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.runway.R
import com.example.runway.databinding.FragmentColourMatcherBinding
import com.example.runway.domain.model.Item
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// Smart colour-matching suggestions for one garment (IIE, 2026).
// Takes an [com.example.runway.ui.navigation.NavArgs.ITEM_ID] argument.

class ColourMatcherFragment : Fragment() {

    private var _binding: FragmentColourMatcherBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val viewModel: ColourMatcherViewModel by viewModels { ColourMatcherViewModel.Factory }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentColourMatcherBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.matcherHeader.title = getString(R.string.rw_colour_matcher_title)
        binding.matcherHeader.setShowBack(true)
        binding.matcherHeader.onBackClick { findNavController().navigateUp() }

        binding.matcherEmpty.title = getString(R.string.rw_colour_matcher_empty_title)
        binding.matcherEmpty.body = getString(R.string.rw_colour_matcher_empty_body)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: ColourMatcherUiState) {
        val reference = state.reference
        if (reference == null) {
            binding.matcherReferenceName.text = getString(
                if (state.isLoading) R.string.rw_item_detail_loading else R.string.rw_item_detail_not_found
            )
            binding.matcherReferenceColour.text = null
            binding.matcherLead.text = null
            return
        }

        binding.matcherReferenceName.text = reference.name
        binding.matcherReferenceColour.text = reference.colour
        binding.matcherLead.text = getString(R.string.rw_colour_matcher_lead, reference.name)

        loadPhoto(reference)
    }

    private fun loadPhoto(item: Item) {
        val path = item.imagePath
        if (path.isNullOrBlank()) {
            binding.matcherReferenceImage.setImageResource(R.drawable.ic_rw_plus)
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                if (File(path).exists()) BitmapFactory.decodeFile(path) else null
            }
            if (bitmap != null) binding.matcherReferenceImage.setImageBitmap(bitmap)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
*/