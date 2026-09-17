package com.example.runway.ui.outfits

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.runway.R
import com.example.runway.databinding.FragmentOutfitDetailBinding
import com.example.runway.domain.model.RatingSummary
import com.example.runway.ui.navigation.NavArgs
import com.example.runway.ui.sheets.ConfidenceSheet
import com.example.runway.ui.components.RunwayDialogs
import com.example.runway.ui.components.RunwayToast
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch

// One saved outfit: its render, its name and its garments, all editable
// (IIE, 2026; Android Open Source Project, 2020c).
// Takes an [com.example.runway.ui.navigation.NavArgs.OUTFIT_ID] argument.

class OutfitDetailFragment : Fragment() {

    private var _binding: FragmentOutfitDetailBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val viewModel: OutfitDetailViewModel by viewModels { OutfitDetailViewModel.Factory }

    // Set while writing the name into the field, so restoring state does not read
    // back as the user typing.
    private var applyingState = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentOutfitDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.outfitDetailHeader.setShowBack(true)
        binding.outfitDetailHeader.onBackClick { findNavController().navigateUp() }

        binding.outfitNameInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (!applyingState) viewModel.onNameChanged(s?.toString().orEmpty())
            }
        })

        binding.outfitSaveButton.setOnClickListener { viewModel.onSave() }
        binding.outfitDeleteButton.setOnClickListener { confirmDelete() }
        binding.outfitWearButton.setOnClickListener { viewModel.onWoreToday() }

        // The sheet saves the rating itself, so we just reload once it closes.
        parentFragmentManager.setFragmentResultListener(
            ConfidenceSheet.TAG, viewLifecycleOwner
        ) { _, _ -> viewModel.onRatingsChanged() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: OutfitDetailUiState) {
        if (state.deleted) {
            findNavController().navigateUp()
            return
        }

        applyingState = true

        binding.outfitDetailHeader.title = state.outfit?.name
            ?: getString(R.string.rw_outfit_loading)

        if (binding.outfitNameInput.text?.toString() != state.name) {
            binding.outfitNameInput.setText(state.name)
        }

        binding.outfitRenderImage.setImageBitmap(state.render)
        binding.outfitRenderImage.isVisible = state.render != null
        binding.outfitNoRender.isVisible = state.render == null && !state.isLoading

        renderGarments(state)
        renderConfidence(state.ratings)

        // A logged wear is what prompts for a rating.
        if (state.wearLoggedAt != null) {
            viewModel.onWearHandled()
            RunwayToast.show(requireView(), getString(R.string.rw_outfit_wear_logged))
            openConfidenceSheet()
        }

        binding.outfitSaveButton.isEnabled = state.hasChanges && !state.isSaving
        binding.outfitSaveButton.setText(
            if (state.isSaving) R.string.rw_outfit_saving else R.string.rw_outfit_save_changes
        )

        state.savedAt?.let {
            RunwayToast.show(requireView(), getString(R.string.rw_outfit_saved))
            viewModel.onMessageShown()
        }
        state.errorMessage?.let {
            RunwayToast.show(requireView(), it)
            viewModel.onMessageShown()
        }

        applyingState = false
    }

    private fun renderConfidence(ratings: RatingSummary) {
        binding.outfitConfidenceStars.rating = ratings.roundedAverage
        binding.outfitConfidenceLabel.text = if (ratings.hasRatings) {
            resources.getQuantityString(
                R.plurals.rw_outfit_confidence_count,
                ratings.count,
                ratings.average.toString(),
                ratings.count,
            )
        } else {
            getString(R.string.rw_outfit_confidence_none)
        }
    }

    private fun openConfidenceSheet() {
        val outfitId = viewModel.uiState.value.outfit?.id ?: return
        findNavController().navigate(
            R.id.action_outfitDetail_to_confidenceSheet,
            bundleOf(NavArgs.OUTFIT_ID to outfitId),
        )
    }

    // A chip per garment; the close icon takes it out of the outfit.
    private fun renderGarments(state: OutfitDetailUiState) {
        val group = binding.outfitGarmentsGroup
        group.removeAllViews()

        binding.outfitNoGarments.isVisible = state.garments.isEmpty() && !state.isLoading

        state.garments.forEach { item ->
            group.addView(
                Chip(requireContext(), null, com.google.android.material.R.attr.chipStyle).apply {
                    text = if (item.size.isNullOrBlank()) item.name else getString(
                        R.string.rw_model_item_with_size, item.name, item.size
                    )
                    isCloseIconVisible = true
                    setOnCloseIconClickListener { viewModel.onRemoveGarment(item) }
                }
            )
        }
    }

    private fun confirmDelete() = RunwayDialogs.confirm(
        context = requireContext(),
        titleRes = R.string.rw_outfit_delete_title,
        bodyRes = R.string.rw_outfit_delete_body,
        confirmLabelRes = R.string.rw_outfit_delete,
        destructive = true,
    ) {
        viewModel.onDelete()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
Android Open Source Project, 2020c. Fragments. [online] Available at: <https://developer.android.com/guide/components/fragments> [Accessed 31 July 2023].
*/
