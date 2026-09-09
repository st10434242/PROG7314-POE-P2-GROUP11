package com.example.runway.ui.sheets

import com.example.runway.R

/** Shows what is planned for one calendar day. Takes a
 * [com.example.runway.ui.navigation.NavArgs.DATE_ISO] argument. Still a placeholder - see
 * [StubBottomSheet]. */
class DayDetailSheet : StubBottomSheet() {
    override val titleRes = R.string.rw_sheet_day_detail_title
    override val bodyRes = R.string.rw_sheet_day_detail_body

    companion object {
        const val TAG = "DayDetailSheet"
    }
}
