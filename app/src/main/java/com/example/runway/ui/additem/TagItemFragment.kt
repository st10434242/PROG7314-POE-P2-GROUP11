package com.example.runway.ui.additem

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.example.runway.R
import com.example.runway.ui.StubFragment
import com.google.android.material.button.MaterialButton

/** Final step of the add-item modal flow: category/colour/brand/size/material/price.
 * Saving here exits the modal stack and lands on Item Detail. Still a placeholder - see
 * [StubFragment]. */
class TagItemFragment : StubFragment() {
    override val titleRes = R.string.rw_stub_tag_item_title
    override val bodyRes = R.string.rw_stub_tag_item_body

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        emptyState.setAction(
            MaterialButton(requireContext()).apply {
                setText(R.string.rw_tag_item_continue)
                setOnClickListener {
                    findNavController().navigate(R.id.action_tagItem_to_itemDetail)
                }
            }
        )
    }
}
