package com.example.runway.ui.outfits

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
import com.example.runway.databinding.FragmentOutfitsBinding
import com.example.runway.domain.model.Outfit
import com.example.runway.ui.components.RunwayToast
import com.example.runway.ui.navigation.NavArgs
import kotlinx.coroutines.launch

// The outfits tab: every outfit the user has saved from the model screen.

class OutfitsFragment : Fragment() {

    private var _binding: FragmentOutfitsBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val viewModel: OutfitsViewModel by viewModels { OutfitsViewModel.Factory }

    private lateinit var adapter: OutfitsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentOutfitsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = OutfitsAdapter(viewLifecycleOwner.lifecycleScope, ::open)
        binding.outfitsList.layoutManager = LinearLayoutManager(requireContext())
        binding.outfitsList.adapter = adapter

        binding.outfitsRefreshButton.setOnClickListener { viewModel.refresh() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    // An outfit may have been saved or edited while this tab was off screen.
    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    private fun render(state: OutfitsUiState) {
        adapter.submitList(state.outfits)

        val isEmpty = state.outfits.isEmpty() && !state.isLoading
        binding.outfitsList.isVisible = !isEmpty
        binding.outfitsEmpty.isVisible = isEmpty
        if (isEmpty) {
            binding.outfitsEmpty.title = getString(R.string.rw_outfits_empty_title)
            binding.outfitsEmpty.body = getString(R.string.rw_outfits_empty_body)
        }

        binding.outfitsProgress.isVisible = state.isLoading
        binding.outfitsRefreshButton.isEnabled = !state.isLoading

        state.errorMessage?.let {
            RunwayToast.show(requireView(), it)
            viewModel.onMessageShown()
        }
    }

    private fun open(outfit: Outfit) {
        findNavController().navigate(
            R.id.action_outfits_to_outfitDetail,
            bundleOf(NavArgs.OUTFIT_ID to outfit.id),
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.outfitsList.adapter = null
        _binding = null
    }
}
