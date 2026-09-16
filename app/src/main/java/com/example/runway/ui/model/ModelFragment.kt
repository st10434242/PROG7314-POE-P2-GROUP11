package com.example.runway.ui.model

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.runway.R
import com.example.runway.data.pose.PoseFailure
import com.example.runway.databinding.FragmentModelBinding
import com.example.runway.domain.model.BodyShape
import com.example.runway.domain.model.BodyType
import com.example.runway.domain.model.ClothingSize
import com.example.runway.domain.model.FitVerdict
import com.example.runway.domain.model.Item
import com.example.runway.domain.model.ModelProfile
import com.example.runway.ui.components.RunwayDialogs
import com.example.runway.ui.components.RunwayToast
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch

// Virtual model screen (IIE, 2026; Android Open Source Project, 2020c).
// The model is the user's photo; the controls collect the measurements a try-on needs.

class ModelFragment : Fragment() {

    private var _binding: FragmentModelBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val viewModel: ModelViewModel by viewModels { ModelViewModel.Factory }

    // Set while applying state to the chips, so programmatic checks do not
    // feed back into the ViewModel as if the user had tapped them.
    private var applyingState = false

    // The wardrobe the chips were last built from, so they are only rebuilt when
    // the wardrobe itself changes rather than on every state emission.
    private var wardrobeChipIds: List<String> = emptyList()

    // The photo picker needs no storage permission: the user chooses the one image
    // and the app is handed only that (Android Open Source Project, 2026).
    private val pickPhoto = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        uri?.let { viewModel.onPhotoPicked(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentModelBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.modelHeader.title = getString(R.string.rw_model_title)
        binding.modelHeader.onBackClick { findNavController().navigateUp() }

        buildSizeChips(binding.modelTopSizeGroup) { viewModel.onTopSizeSelected(it) }
        buildSizeChips(binding.modelBottomSizeGroup) { viewModel.onBottomSizeSelected(it) }

        binding.modelHeightSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) viewModel.onHeightChanged(value.toInt())
        }
        binding.modelWeightSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) viewModel.onWeightChanged(value.toInt())
        }
        binding.modelShoeSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) viewModel.onShoeSizeChanged(value.toInt())
        }

        binding.modelSaveButton.setOnClickListener { viewModel.onSave() }
        binding.modelClearButton.setOnClickListener { viewModel.onClearGarments() }
        binding.modelPhotoButton.setOnClickListener {
            pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        binding.modelRemovePhotoButton.setOnClickListener { viewModel.onRemovePhoto() }
        binding.modelRenderButton.setOnClickListener { startRender() }
        binding.modelClearRenderButton.setOnClickListener { viewModel.onClearRender() }
        binding.modelSaveOutfitButton.setOnClickListener { viewModel.onSaveOutfit() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun buildSizeChips(group: ChipGroup, onPicked: (ClothingSize) -> Unit) =
        ClothingSize.entries.forEach { size ->
            group.addView(
                Chip(requireContext(), null, com.google.android.material.R.attr.chipStyle).apply {
                    text = size.label
                    isCheckable = true
                    tag = size
                    setOnCheckedChangeListener { _, checked -> if (checked && !applyingState) onPicked(size) }
                }
            )
        }

    private fun render(state: ModelUiState) {
        applyingState = true

        binding.modelPreview.photo = state.photo
        binding.modelPreview.renderedImage = state.renderedImage
        binding.modelPhotoEmpty.isVisible = !state.hasPhoto && !state.isAnalysingPhoto

        renderPhotoControls(state)
        renderRenderControls(state)
        renderSizes(state)
        renderWardrobe(state)
        renderWornSummary(state)

        binding.modelSaveButton.isEnabled = !state.isSaving
        binding.modelSaveButton.text = getString(
            if (state.isSaving) R.string.rw_model_saving else R.string.rw_model_save
        )

        state.savedAt?.let {
            RunwayToast.show(requireView(), getString(R.string.rw_model_saved))
            viewModel.onMessageShown()
        }
        state.savedOutfitAt?.let {
            RunwayToast.show(requireView(), getString(R.string.rw_model_outfit_saved))
            viewModel.onMessageShown()
        }
        state.errorMessage?.let {
            RunwayToast.show(requireView(), it)
            viewModel.onMessageShown()
        }

        applyingState = false
    }

    private fun renderPhotoControls(state: ModelUiState) {
        binding.modelPhotoButton.isEnabled = !state.isAnalysingPhoto
        binding.modelPhotoButton.text = getString(
            when {
                state.isAnalysingPhoto -> R.string.rw_model_photo_analysing
                state.hasPhoto -> R.string.rw_model_photo_replace
                else -> R.string.rw_model_photo_add
            }
        )
        binding.modelRemovePhotoButton.isVisible = state.hasPhoto && !state.isAnalysingPhoto
        binding.modelPhotoHint.text = getString(
            if (state.hasPhoto) R.string.rw_model_photo_hint_on else R.string.rw_model_photo_hint_off
        )

        val type = state.profile.bodyType
        val shape = state.profile.bodyShape
        binding.modelBuildLabel.isVisible = state.hasPhoto && type != null && shape != null
        if (type != null && shape != null) {
            binding.modelBuildLabel.text =
                getString(R.string.rw_model_build_detected, getString(labelFor(type)), getString(labelFor(shape)))
        }

        if (state.buildDetected) {
            RunwayToast.show(requireView(), getString(R.string.rw_model_photo_read))
            viewModel.onMessageShown()
        }
        state.photoFailure?.let {
            RunwayToast.show(requireView(), getString(messageFor(it)))
            viewModel.onMessageShown()
        }
    }

    private fun renderRenderControls(state: ModelUiState) {
        val garments = viewModel.renderableGarments()

        binding.modelRenderButton.isEnabled =
            state.hasPhoto && garments.isNotEmpty() && !state.isRendering
        binding.modelRenderButton.setText(
            if (state.isRendering) R.string.rw_model_rendering else R.string.rw_model_render
        )
        binding.modelClearRenderButton.isVisible = state.isShowingRender && !state.isRendering

        binding.modelSaveOutfitButton.isEnabled = state.canSaveOutfit
        binding.modelSaveOutfitButton.setText(
            if (state.isSavingOutfit) R.string.rw_model_saving_outfit else R.string.rw_model_save_outfit
        )

        // Says up front what a render will cost in time, because twenty seconds
        // of a blank button looks like a hang.
        binding.modelRenderHint.text = when {
            !state.hasPhoto -> getString(R.string.rw_model_render_needs_photo)
            state.isRendering -> getString(R.string.rw_model_render_working)
            garments.isEmpty() -> getString(R.string.rw_model_render_needs_garment)
            else -> resources.getQuantityString(
                R.plurals.rw_model_render_ready, garments.size, garments.size
            )
        }
    }

    // The body photo leaves the device for this, so it is asked for once and
    // never assumed.
    private fun startRender() {
        if (!viewModel.needsRenderConsent) {
            viewModel.onRender()
            return
        }
        RunwayDialogs.confirm(
            context = requireContext(),
            titleRes = R.string.rw_model_render_consent_title,
            bodyRes = R.string.rw_model_render_consent_body,
            confirmLabelRes = R.string.rw_model_render_consent_confirm,
        ) {
            viewModel.onRenderConsentGiven()
            viewModel.onRender()
        }
    }

    private fun renderSizes(state: ModelUiState) {
        check(binding.modelTopSizeGroup, state.profile.topSize)
        check(binding.modelBottomSizeGroup, state.profile.bottomSize)

        val height = state.profile.heightCm
            .coerceIn(ModelProfile.MIN_HEIGHT_CM, ModelProfile.MAX_HEIGHT_CM)
        binding.modelHeightSlider.value = height.toFloat()
        binding.modelHeightLabel.text = getString(R.string.rw_model_height, height)

        val weight = (state.profile.weightKg ?: ModelProfile.DEFAULT_WEIGHT_KG)
            .coerceIn(ModelProfile.MIN_WEIGHT_KG, ModelProfile.MAX_WEIGHT_KG)
        binding.modelWeightSlider.value = weight.toFloat()
        binding.modelWeightLabel.text = if (state.profile.weightKg == null) {
            getString(R.string.rw_model_weight_unset)
        } else {
            getString(R.string.rw_model_weight, weight)
        }

        val shoe = (state.profile.shoeSizeEu ?: ModelProfile.DEFAULT_SHOE_EU)
            .coerceIn(ModelProfile.MIN_SHOE_EU, ModelProfile.MAX_SHOE_EU)
        binding.modelShoeSlider.value = shoe.toFloat()
        binding.modelShoeLabel.text = if (state.profile.shoeSizeEu == null) {
            getString(R.string.rw_model_shoe_unset)
        } else {
            getString(R.string.rw_model_shoe, shoe)
        }
    }

    // A chip per wardrobe item, checked when that garment is on the model.
    // Rebuilding every chip on every emission destroys the chip the user is
    // touching, because a tap changes the state and the state redraws the group.
    // The chips are therefore built once per wardrobe and only re-checked after that.
    private fun renderWardrobe(state: ModelUiState) {
        val group = binding.modelWardrobeGroup

        binding.modelWardrobeEmpty.isVisible = state.wardrobe.isEmpty() && !state.isLoading
        if (state.wardrobe.isEmpty()) {
            group.removeAllViews()
            wardrobeChipIds = emptyList()
            binding.modelWardrobeEmpty.title = getString(R.string.rw_model_empty_title)
            binding.modelWardrobeEmpty.body = getString(R.string.rw_model_empty_body)
            return
        }

        val ids = state.wardrobe.map { it.id }
        if (ids != wardrobeChipIds) {
            group.removeAllViews()
            state.wardrobe.forEach { item ->
                group.addView(
                    Chip(requireContext(), null, com.google.android.material.R.attr.chipStyle).apply {
                        text = if (item.size.isNullOrBlank()) item.name else getString(
                            R.string.rw_model_item_with_size, item.name, item.size
                        )
                        isCheckable = true
                        tag = item.id
                        setOnClickListener { viewModel.onToggleGarment(item) }
                    }
                )
            }
            wardrobeChipIds = ids
        }

        // Checked state is applied to the chips that already exist, which leaves
        // the view the user just pressed alive and animating.
        for (i in 0 until group.childCount) {
            val chip = group.getChildAt(i) as? Chip ?: continue
            val worn = state.worn.any { it.id == chip.tag }
            if (chip.isChecked != worn) chip.isChecked = worn
        }
    }

    // Says how each worn garment's size compares with the user's own, which is the
    // first thing the sizes are useful for even before a try-on render exists.
    private fun renderWornSummary(state: ModelUiState) {
        binding.modelWornSummary.text = if (state.worn.isEmpty()) {
            getString(R.string.rw_model_nothing_on)
        } else {
            state.worn.joinToString("\n") { item -> fitLine(state, item) }
        }
    }

    private fun fitLine(state: ModelUiState, item: Item): String {
        val verdict = state.fitFor(item)
            ?: return getString(R.string.rw_model_fit_unknown, item.name)
        return getString(R.string.rw_model_fit_known, item.name, getString(labelFor(verdict)))
    }

    private fun check(group: ChipGroup, value: Any?) {
        for (i in 0 until group.childCount) {
            val chip = group.getChildAt(i) as? Chip ?: continue
            chip.isChecked = value != null && chip.tag == value
        }
    }

    // Each failure gets its own wording, because each one needs a different photo.
    private fun messageFor(failure: PoseFailure): Int = when (failure) {
        PoseFailure.NO_PERSON -> R.string.rw_model_photo_no_person
        PoseFailure.PARTIAL_BODY -> R.string.rw_model_photo_partial
        PoseFailure.UNREADABLE -> R.string.rw_model_photo_unreadable
    }

    private fun labelFor(verdict: FitVerdict): Int = when (verdict) {
        FitVerdict.TIGHT -> R.string.rw_model_fit_tight
        FitVerdict.SNUG -> R.string.rw_model_fit_snug
        FitVerdict.TRUE_TO_SIZE -> R.string.rw_model_fit_true
        FitVerdict.LOOSE -> R.string.rw_model_fit_loose
        FitVerdict.OVERSIZED -> R.string.rw_model_fit_oversized
    }

    private fun labelFor(shape: BodyShape): Int = when (shape) {
        BodyShape.RECTANGLE -> R.string.rw_model_shape_rectangle
        BodyShape.TRIANGLE -> R.string.rw_model_shape_triangle
        BodyShape.INVERTED_TRIANGLE -> R.string.rw_model_shape_inverted
        BodyShape.HOURGLASS -> R.string.rw_model_shape_hourglass
        BodyShape.OVAL -> R.string.rw_model_shape_oval
    }

    private fun labelFor(type: BodyType): Int = when (type) {
        BodyType.SLIM -> R.string.rw_model_type_slim
        BodyType.AVERAGE -> R.string.rw_model_type_average
        BodyType.ATHLETIC -> R.string.rw_model_type_athletic
        BodyType.FULL -> R.string.rw_model_type_full
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
Android Open Source Project, 2020c. Fragments. [online] Available at: <https://developer.android.com/guide/components/fragments> [Accessed 31 July 2023].
Android Open Source Project, 2026. Photo picker. [online] Available at: <https://developer.android.com/training/data-storage/shared/photopicker> [Accessed 15 September 2026].
*/
