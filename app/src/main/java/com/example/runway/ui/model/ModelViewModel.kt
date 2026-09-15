package com.example.runway.ui.model

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.runway.RunwayApplication
import com.example.runway.data.local.BodyPhoto
import com.example.runway.data.local.ModelPhotoStore
import com.example.runway.data.local.OutfitRenderStore
import com.example.runway.data.pose.PoseAnalyzer
import com.example.runway.data.pose.PoseDetectionException
import com.example.runway.data.pose.PoseFailure
import com.example.runway.data.remote.api.toUserMessage
import com.example.runway.data.repository.TryOnRepository
import com.example.runway.domain.model.ClothingSize
import com.example.runway.domain.model.Item
import com.example.runway.domain.model.ModelProfile
import com.example.runway.domain.model.Outfit
import com.example.runway.domain.repository.ItemRepository
import com.example.runway.domain.repository.ModelRepository
import com.example.runway.domain.repository.OutfitRepository
import com.example.runway.ui.navigation.NavArgs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ViewModel for the virtual model screen (IIE, 2026).
// Selection changes are applied locally and only written on save (SCRUM-84, 85, 86).

class ModelViewModel(
    private val modelRepository: ModelRepository,
    private val itemRepository: ItemRepository,
    private val photoStore: ModelPhotoStore,
    private val poseAnalyzer: PoseAnalyzer,
    private val tryOnRepository: TryOnRepository,
    private val outfitRepository: OutfitRepository,
    private val outfitRenderStore: OutfitRenderStore,
    // Set when the screen was opened from a garment's detail page: that garment
    // goes on the model as soon as the wardrobe has loaded.
    savedStateHandle: SavedStateHandle? = null,
) : ViewModel() {

    private var pendingGarmentId: String? = savedStateHandle?.get<String>(NavArgs.ITEM_ID)

    private val _uiState = MutableStateFlow(
        // The cached profile draws immediately; the network read refines it.
        ModelUiState(profile = modelRepository.cachedProfile(), isLoading = true)
    )
    val uiState: StateFlow<ModelUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
        loadPhoto()
        observeWardrobe()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            modelRepository.getProfile()
                .onSuccess { saved -> _uiState.update { it.copy(profile = saved, isLoading = false) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, errorMessage = e.toUserMessage()) } }
        }
    }

    // The photo lives on this device only, so it is read from local storage rather
    // than the API and survives being offline.
    private fun loadPhoto() {
        viewModelScope.launch {
            val stored = photoStore.load() ?: return@launch
            _uiState.update { it.copy(photo = stored) }
        }
    }

    private fun observeWardrobe() {
        viewModelScope.launch {
            itemRepository.observeItems().collect { items ->
                _uiState.update { state ->
                    // Anything removed from the wardrobe stops being worn.
                    val stillThere = state.worn.filter { worn -> items.any { it.id == worn.id } }

                    // Applied once, as soon as the wardrobe arrives. After that the
                    // user's own choices stand and this never fires again.
                    val requested = pendingGarmentId?.let { id -> items.firstOrNull { it.id == id } }
                    if (requested != null) pendingGarmentId = null

                    val worn = when {
                        requested == null -> stillThere
                        stillThere.any { it.id == requested.id } -> stillThere
                        // One garment per category, the same rule a tap follows.
                        else -> stillThere.filterNot {
                            it.category.equals(requested.category, ignoreCase = true)
                        } + requested
                    }

                    state.copy(wardrobe = items, worn = worn, renderedImage = null)
                }
            }
        }
    }

    // A full-body photo picked by the user. Nothing is kept unless a body is found
    // in it, so a rejected photo leaves no trace on the device.
    fun onPhotoPicked(uri: Uri) {
        _uiState.update { it.copy(isAnalysingPhoto = true, photoFailure = null, buildDetected = false) }
        viewModelScope.launch {
            val bitmap = photoStore.decode(uri)
            if (bitmap == null) {
                _uiState.update { it.copy(isAnalysingPhoto = false, photoFailure = PoseFailure.UNREADABLE) }
                return@launch
            }

            poseAnalyzer.analyse(bitmap)
                .onSuccess { landmarks ->
                    photoStore.save(bitmap, landmarks)
                    _uiState.update { state ->
                        state.copy(
                            photo = BodyPhoto(bitmap, landmarks),
                            isAnalysingPhoto = false,
                            photoFailure = null,
                            // Build and silhouette are read off the photo rather than
                            // chosen. They are context for the try-on, not a measurement.
                            profile = state.profile.copy(
                                bodyType = landmarks.suggestedBodyType(),
                                bodyShape = landmarks.suggestedBodyShape(),
                            ),
                            buildDetected = true,
                        )
                    }
                }
                .onFailure { error ->
                    bitmap.recycle()
                    val failure = (error as? PoseDetectionException)?.failure ?: PoseFailure.UNREADABLE
                    _uiState.update { it.copy(isAnalysingPhoto = false, photoFailure = failure) }
                }
        }
    }

    // Removes the image from the device. The detected build goes with it: it was
    // read off that photo and means nothing without it.
    fun onRemovePhoto() {
        photoStore.clear()
        _uiState.update { state ->
            state.copy(
                photo = null,
                photoFailure = null,
                buildDetected = false,
                profile = state.profile.copy(bodyType = null, bodyShape = null),
            )
        }
    }

    // The sizes the user actually wears. These are what a try-on reasons about,
    // alongside the size printed on each garment.
    fun onTopSizeSelected(size: ClothingSize) =
        _uiState.update { it.copy(profile = it.profile.copy(topSize = size)) }

    fun onBottomSizeSelected(size: ClothingSize) =
        _uiState.update { it.copy(profile = it.profile.copy(bottomSize = size)) }

    fun onHeightChanged(cm: Int) = _uiState.update {
        it.copy(profile = it.profile.copy(
            heightCm = cm.coerceIn(ModelProfile.MIN_HEIGHT_CM, ModelProfile.MAX_HEIGHT_CM)
        ))
    }

    fun onWeightChanged(kg: Int) = _uiState.update {
        it.copy(profile = it.profile.copy(
            weightKg = kg.coerceIn(ModelProfile.MIN_WEIGHT_KG, ModelProfile.MAX_WEIGHT_KG)
        ))
    }

    fun onShoeSizeChanged(eu: Int) = _uiState.update {
        it.copy(profile = it.profile.copy(
            shoeSizeEu = eu.coerceIn(ModelProfile.MIN_SHOE_EU, ModelProfile.MAX_SHOE_EU)
        ))
    }

    // One garment per category: putting on a second top replaces the first.
    fun onToggleGarment(item: Item) = _uiState.update { state ->
        val alreadyOn = state.worn.any { it.id == item.id }
        val worn = if (alreadyOn) {
            state.worn.filterNot { it.id == item.id }
        } else {
            state.worn.filterNot { it.category.equals(item.category, ignoreCase = true) } + item
        }
        state.copy(worn = worn, renderedImage = null)
    }

    fun onClearGarments() = _uiState.update { it.copy(worn = emptyList()) }

    // --- Photoreal render -----------------------------------------------------

    val needsRenderConsent: Boolean get() = !tryOnRepository.hasConsented

    fun onRenderConsentGiven() { tryOnRepository.hasConsented = true }

    // The garments the image service can actually put on a person: it renders
    // clothing, not shoes or accessories, and only two at a time.
    fun renderableGarments(): List<Item> =
        _uiState.value.worn.filter { tryOnRepository.canRender(it) }.take(MAX_RENDER_GARMENTS)

    fun onRender() {
        val photo = _uiState.value.photo
        if (photo == null) {
            _uiState.update { it.copy(errorMessage = "Add a photo of yourself first") }
            return
        }

        val garments = renderableGarments()
        if (garments.isEmpty()) {
            _uiState.update {
                it.copy(errorMessage = "Put on a top, bottom or coat that has a photo of its own")
            }
            return
        }

        _uiState.update { it.copy(isRendering = true, errorMessage = null) }
        viewModelScope.launch {
            tryOnRepository.render(photo.bitmap, garments)
                .onSuccess { image ->
                    _uiState.update { it.copy(renderedImage = image, isRendering = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isRendering = false, errorMessage = e.toUserMessage()) }
                }
        }
    }

    // Goes back to the flat preview, which is what the user adjusts against.
    fun onClearRender() = _uiState.update { it.copy(renderedImage = null) }

    // --- Saving an outfit -----------------------------------------------------

    // Keeps what is on the model as a named outfit, together with the render if
    // one has been made. The render stays on this device; the outfit itself syncs.
    fun onSaveOutfit() {
        val state = _uiState.value
        if (state.worn.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Put something on the model first") }
            return
        }

        _uiState.update { it.copy(isSavingOutfit = true, errorMessage = null) }
        viewModelScope.launch {
            val renderPath = state.renderedImage?.let { outfitRenderStore.save(it) }

            outfitRepository.create(
                Outfit(
                    name = nameFor(state.worn),
                    itemIds = state.worn.map { it.id },
                    renderPath = renderPath,
                )
            )
                .onSuccess {
                    _uiState.update {
                        it.copy(isSavingOutfit = false, savedOutfitAt = System.currentTimeMillis())
                    }
                }
                .onFailure { e ->
                    // The render was written before the outfit; without an outfit
                    // to belong to it would sit on disk forever.
                    outfitRenderStore.delete(renderPath)
                    _uiState.update { it.copy(isSavingOutfit = false, errorMessage = e.toUserMessage()) }
                }
        }
    }

    // A name the user will recognise in the list, which they can change later.
    private fun nameFor(worn: List<Item>): String =
        worn.joinToString(" + ") { it.name }.take(MAX_OUTFIT_NAME)

    // SCRUM-86.
    fun onSave() {
        val profile = _uiState.value.profile
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            modelRepository.saveProfile(profile)
                .onSuccess { saved ->
                    _uiState.update { it.copy(profile = saved, isSaving = false, savedAt = System.currentTimeMillis()) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isSaving = false, errorMessage = e.toUserMessage()) }
                }
        }
    }

    fun onMessageShown() = _uiState.update {
        it.copy(
            errorMessage = null,
            savedAt = null,
            savedOutfitAt = null,
            photoFailure = null,
            buildDetected = false,
        )
    }

    override fun onCleared() {
        super.onCleared()
        poseAnalyzer.close()
    }

    companion object {
        // The API refuses more than this, and each one is a separate paid render.
        const val MAX_RENDER_GARMENTS = 2

        // The API caps an outfit name at 120 characters.
        const val MAX_OUTFIT_NAME = 120

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as RunwayApplication
                ModelViewModel(
                    modelRepository = app.container.modelRepository,
                    itemRepository = app.container.itemRepository,
                    photoStore = app.container.modelPhotoStore,
                    poseAnalyzer = PoseAnalyzer(),
                    tryOnRepository = app.container.tryOnRepository,
                    outfitRepository = app.container.outfitRepository,
                    outfitRenderStore = app.container.outfitRenderStore,
                    savedStateHandle = createSavedStateHandle(),
                )
            }
        }
    }
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
*/
