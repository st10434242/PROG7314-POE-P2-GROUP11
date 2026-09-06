package com.example.runway.ui.additem

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.example.runway.R
import com.example.runway.ui.StubFragment
import com.google.android.material.button.MaterialButton

/** First step of the add-item modal flow: CameraX capture or gallery pick.
 * Still a placeholder - see [StubFragment]. */
class CameraFragment : StubFragment() {
    override val titleRes = R.string.rw_stub_camera_title
    override val bodyRes = R.string.rw_stub_camera_body

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        emptyState.setAction(
            MaterialButton(requireContext()).apply {
                setText(R.string.rw_camera_continue)
                setOnClickListener {
                    findNavController().navigate(R.id.action_camera_to_cutout)
                }
            }
        )
    }
}
