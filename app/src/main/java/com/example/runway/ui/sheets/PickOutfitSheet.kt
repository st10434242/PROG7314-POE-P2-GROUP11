package com.example.runway.ui.sheets

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.runway.R
import com.example.runway.ui.components.EmptyStateView
import com.example.runway.ui.components.ListRowView
import com.example.runway.ui.components.RunwayBottomSheet
import com.example.runway.ui.components.RunwayToast
import com.example.runway.ui.outfits.PlanDates
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.launch

// Plans an outfit onto the calendar: pick an outfit for a day, or a day for an outfit.
class PickOutfitSheet : RunwayBottomSheet(R.layout.sheet_pick_outfit) {

    private val viewModel: PickOutfitViewModel by viewModels { PickOutfitViewModel.Factory }

    override val sheetTitle: CharSequence
        get() = getString(
            if (viewModel.uiState.value.choosingOutfit) R.string.rw_sheet_pick_outfit_title
            else R.string.rw_plan_schedule_title
        )

    // Rows are only rebuilt when what they show changes, not on every state update.
    private var shownRows: Any? = null

    override fun onContentCreated(content: View) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { render(content, it) }
            }
        }
    }

    private fun render(content: View, state: PickOutfitUiState) {
        val subtitle = content.findViewById<TextView>(R.id.pickSubtitle)
        subtitle.isVisible = state.date != null
        state.date?.let { subtitle.text = PlanDates.label(requireContext(), it) }

        content.findViewById<LinearProgressIndicator>(R.id.pickLoading).isVisible = state.isLoading || state.isSaving

        val empty = content.findViewById<EmptyStateView>(R.id.pickEmpty)
        val nothingToPick = state.choosingOutfit && state.outfits.isEmpty()
        empty.isVisible = !state.isLoading && (state.loadFailed || nothingToPick)
        if (state.loadFailed) {
            empty.title = getString(R.string.rw_plan_load_failed)
            empty.body = null
        } else {
            empty.title = getString(R.string.rw_plan_no_outfits_title)
            empty.body = getString(R.string.rw_plan_no_outfits_body)
        }

        val options = content.findViewById<LinearLayout>(R.id.pickOptions)
        val rowsKey = if (state.choosingOutfit) state.outfits else state.days to state.plannedByDay
        if (!state.isLoading && rowsKey != shownRows) {
            shownRows = rowsKey
            options.removeAllViews()
            if (state.choosingOutfit) addOutfitRows(options, state) else addDayRows(options, state)
        }
        for (i in 0 until options.childCount) options.getChildAt(i).isEnabled = !state.isSaving

        state.plannedFor?.let { date ->
            RunwayToast.show(content, getString(R.string.rw_plan_planned_for, PlanDates.label(requireContext(), date)))
            // Lets the planner, or whatever opened this, reload.
            setFragmentResult(RESULT_KEY, bundleOf())
            dismiss()
        }

        state.errorMessage?.let {
            RunwayToast.show(content, it, R.drawable.ic_rw_alert)
            viewModel.onMessageShown()
        }
    }

    private fun addOutfitRows(options: LinearLayout, state: PickOutfitUiState) = state.outfits.forEach { outfit ->
        options.addView(ListRowView(requireContext()).apply {
            setIcon(R.drawable.ic_rw_sparkles)
            label = outfit.name
            value = outfit.occasion
            showChevron = true
            onClick { viewModel.onPickOutfit(outfit.id) }
        })
    }

    private fun addDayRows(options: LinearLayout, state: PickOutfitUiState) = state.days.forEach { day ->
        options.addView(ListRowView(requireContext()).apply {
            setIcon(R.drawable.ic_rw_calendar)
            label = PlanDates.label(requireContext(), day)
            // Says what's there already, since picking the day replaces it.
            value = state.plannedByDay[day]?.let { getString(R.string.rw_plan_replaces, it.name) }
            showChevron = true
            onClick { viewModel.onPickDay(day) }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        shownRows = null
    }

    companion object {
        const val TAG = "PickOutfitSheet"

        // Sent by both planner sheets whenever a day changes.
        const val RESULT_KEY = "planChanged"
    }
}
