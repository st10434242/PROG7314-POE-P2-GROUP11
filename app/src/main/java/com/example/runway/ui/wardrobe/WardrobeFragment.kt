package com.example.runway.ui.wardrobe

import android.os.Bundle
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.runway.R
import com.example.runway.databinding.FragmentWardrobeBinding
import com.example.runway.ui.components.RunwayToast
import com.example.runway.ui.items.ItemsUiState
import com.example.runway.ui.items.ItemsViewModel
import com.example.runway.ui.navigation.NavArgs
import kotlinx.coroutines.launch

// The wardrobe tab: every garment the user owns.
// Room is the source of truth, so a newly saved item appears here before the
// server has heard about it.

class WardrobeFragment : Fragment() {

    private var _binding: FragmentWardrobeBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val viewModel: ItemsViewModel by viewModels { ItemsViewModel.Factory }

    private lateinit var adapter: WardrobeAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentWardrobeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = WardrobeAdapter(viewLifecycleOwner.lifecycleScope, ::openItem)
        binding.wardrobeList.layoutManager = LinearLayoutManager(requireContext())
        binding.wardrobeList.adapter = adapter

        binding.wardrobeRefreshButton.setOnClickListener { viewModel.refresh() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: ItemsUiState) {
        adapter.submitList(state.items)

        val isEmpty = state.items.isEmpty() && !state.isLoading
        binding.wardrobeList.isVisible = !isEmpty
        binding.wardrobeEmpty.isVisible = isEmpty
        if (isEmpty) {
            binding.wardrobeEmpty.title = getString(R.string.rw_wardrobe_empty_title)
            binding.wardrobeEmpty.body = getString(R.string.rw_wardrobe_empty_body)
        }

        binding.wardrobeProgress.isVisible = state.isSyncing
        binding.wardrobeRefreshButton.isEnabled = !state.isSyncing

        binding.wardrobeCount.text = resources.getQuantityString(
            R.plurals.rw_wardrobe_count, state.items.size, state.items.size
        )

        // Says plainly that some rows are only on this phone so far.
        binding.wardrobeUnsynced.isVisible = state.hasUnsyncedChanges
        binding.wardrobeUnsynced.setText(R.string.rw_wardrobe_unsynced)

        state.errorMessage?.let {
            RunwayToast.show(requireView(), it)
            viewModel.onErrorShown()
        }
    }

    private fun openItem(itemId: com.example.runway.domain.model.Item) {
        findNavController().navigate(
            R.id.action_wardrobe_to_itemDetail,
            bundleOf(NavArgs.ITEM_ID to itemId.id),
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // The adapter holds decoded bitmaps; dropping it with the view frees them.
        binding.wardrobeList.adapter = null
        _binding = null
    }
}
