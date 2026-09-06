package com.example.runway.ui.additem

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.example.runway.R
import com.example.runway.ui.StubFragment
import com.google.android.material.button.MaterialButton

/** Second step of the add-item modal flow: automatic background-removal preview.
 * Still a placeholder - see [StubFragment]. */
class CutoutFragment : StubFragment() {
    override val titleRes = R.string.rw_stub_cutout_title
    override val bodyRes = R.string.rw_stub_cutout_body

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        emptyState.setAction(
            MaterialButton(requireContext()).apply {
                setText(R.string.rw_cutout_continue)
                setOnClickListener {
                    findNavController().navigate(R.id.action_cutout_to_tagItem)
                }
            }
        )
    }
}
