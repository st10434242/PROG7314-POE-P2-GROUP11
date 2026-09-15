package com.example.runway.ui.additem

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.runway.R
import com.example.runway.databinding.FragmentTagItemBinding
import com.example.runway.ui.components.RunwayToast
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch

// Final step of adding an item: name it, say what it is, and save (IIE, 2026).
// Category is a fixed list because the model preview and the API both key off it.

class TagItemFragment : Fragment() {

    private var _binding: FragmentTagItemBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val viewModel: AddItemViewModel by activityViewModels { AddItemViewModel.Factory }

    private var category: String = CATEGORIES.first()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentTagItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tagHeader.title = getString(R.string.rw_tag_item_title)
        binding.tagHeader.onBackClick { findNavController().navigateUp() }

        buildCategoryChips()
        binding.tagPreview.setImageBitmap(viewModel.state.value.chosen)

        binding.tagSaveButton.setOnClickListener { save() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }
    }

    private fun buildCategoryChips() = CATEGORIES.forEachIndexed { index, value ->
        binding.tagCategoryGroup.addView(
            Chip(requireContext(), null, com.google.android.material.R.attr.chipStyle).apply {
                text = getString(labelFor(value))
                isCheckable = true
                isChecked = index == 0
                setOnCheckedChangeListener { _, checked -> if (checked) category = value }
            }
        )
    }

    private fun save() = viewModel.onSave(
        name = binding.tagNameInput.text?.toString().orEmpty(),
        category = category,
        colour = binding.tagColourInput.text?.toString(),
        brand = binding.tagBrandInput.text?.toString(),
        size = binding.tagSizeInput.text?.toString(),
        // A blank or unparseable price is simply no price, not an error.
        purchasePrice = binding.tagPriceInput.text?.toString()?.toDoubleOrNull(),
    )

    private fun render(state: AddItemState) {
        binding.tagSaveButton.isEnabled = !state.isSaving
        binding.tagSaveButton.setText(if (state.isSaving) R.string.rw_tag_saving else R.string.rw_tag_save)

        state.errorMessage?.let {
            RunwayToast.show(requireView(), it)
            viewModel.onMessageShown()
        }

        if (state.saved) {
            RunwayToast.show(requireView(), getString(R.string.rw_tag_saved))
            viewModel.reset()
            // Closes the whole add-item stack and lands back on the tab the user
            // started from, where the new item is already in the list.
            findNavController().popBackStack(R.id.addItemGraph, true)
        }
    }

    private fun labelFor(value: String): Int = when (value) {
        "TOP" -> R.string.rw_category_top
        "BOTTOM" -> R.string.rw_category_bottom
        "OUTERWEAR" -> R.string.rw_category_outerwear
        "SHOES" -> R.string.rw_category_shoes
        else -> R.string.rw_category_other
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        // The four the model preview can draw, plus a catch-all.
        val CATEGORIES = listOf("TOP", "BOTTOM", "OUTERWEAR", "SHOES", "OTHER")
    }
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
*/
