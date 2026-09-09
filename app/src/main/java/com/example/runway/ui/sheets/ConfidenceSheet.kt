package com.example.runway.ui.sheets

import com.example.runway.R

/** "How confident did you feel today?" rating, shown after marking an outfit worn. Takes an
 * [com.example.runway.ui.navigation.NavArgs.OUTFIT_ID] argument. Still a placeholder - see
 * [StubBottomSheet]. */
class ConfidenceSheet : StubBottomSheet() {
    override val titleRes = R.string.rw_sheet_confidence_title
    override val bodyRes = R.string.rw_sheet_confidence_body

    companion object {
        const val TAG = "ConfidenceSheet"
    }
}
