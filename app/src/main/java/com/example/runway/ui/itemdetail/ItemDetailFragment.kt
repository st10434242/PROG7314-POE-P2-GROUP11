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

// Detail screen for one garment (IIE, 2026; Android Open Source Project, 2020c).

// Takes an [com.example.runway.ui.navigation.NavArgs.ITEM_ID] argument.
class ItemDetailFragment : StubFragment() {
    override val titleRes = R.string.rw_stub_item_detail_title
    override val bodyRes = R.string.rw_stub_item_detail_body

    private val viewModel: ItemDetailViewModel by viewModels { ItemDetailViewModel.Factory }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val item = state.item
                    emptyState.body = when {
                        state.isLoading -> getString(R.string.rw_item_detail_loading)
                        item == null -> getString(R.string.rw_item_detail_not_found)
                        else -> item.name
                    }
                }
            }
        }
    }
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
Android Open Source Project, 2020c. Fragments. [online] Available at: <https://developer.android.com/guide/components/fragments> [Accessed 31 July 2023].
*/
