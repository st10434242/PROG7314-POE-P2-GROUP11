package com.example.runway.ui.sheets

import com.example.runway.R

/** Schedules an outfit onto the planner. Takes optional
 * [com.example.runway.ui.navigation.NavArgs.DATE_ISO] / [com.example.runway.ui.navigation.NavArgs.OUTFIT_ID]
 * arguments. Still a placeholder - see [StubBottomSheet]. */
class PickOutfitSheet : StubBottomSheet() {
    override val titleRes = R.string.rw_sheet_pick_outfit_title
    override val bodyRes = R.string.rw_sheet_pick_outfit_body

    companion object {
        const val TAG = "PickOutfitSheet"
    }
}
