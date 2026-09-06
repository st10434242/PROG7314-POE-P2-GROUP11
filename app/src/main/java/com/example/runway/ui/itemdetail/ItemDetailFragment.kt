package com.example.runway.ui.itemdetail

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.runway.R
import com.example.runway.ui.StubFragment
import kotlinx.coroutines.launch

/**
 * Takes an [com.example.runway.ui.navigation.NavArgs.ITEM_ID] argument. Already wired to the
 * real [ItemDetailViewModel] - only the screen itself is still a placeholder.
 */
class ItemDetailFragment : StubFragment() {
    override val titleRes = R.string.rw_stub_item_detail_title
    override val bodyRes = R.string.rw_stub_item_detail_body

    private val viewModel: ItemDetailViewModel by viewModels { ItemDetailViewModel.Factory }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    emptyState.body = when {
                        state.isLoading -> getString(R.string.rw_item_detail_loading)
                        state.item == null -> getString(R.string.rw_item_detail_not_found)
                        else -> state.item.title
                    }
                }
            }
        }
    }
}
