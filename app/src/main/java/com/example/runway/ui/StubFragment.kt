package com.example.runway.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.example.runway.databinding.FragmentStubBinding
import com.example.runway.ui.catalog.CatalogActivity

/**
 * Placeholder for a tab root that has not been built yet. Each tab subclasses
 * this and supplies its own title and body text.
 */
abstract class StubFragment : Fragment() {

    /** Title shown in the header and the empty state. */
    @get:StringRes
    protected abstract val titleRes: Int

    /** Short description of what will live on this screen. */
    @get:StringRes
    protected abstract val bodyRes: Int

    private var _binding: FragmentStubBinding? = null
    private val binding get() = requireNotNull(_binding) { "Accessed binding outside the view lifecycle" }

    // Named logTag, not tag: Fragment already has a tag property.
    private val logTag: String get() = javaClass.simpleName

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        Log.d(logTag, "onCreateView")
        _binding = FragmentStubBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(logTag, "onViewCreated")

        val title = getString(titleRes)
        binding.stubHeader.title = title
        binding.stubEmpty.title = title
        binding.stubEmpty.body = getString(bodyRes)

        binding.stubCatalog.setOnClickListener {
            Log.d(logTag, "opening the component catalog")
            startActivity(Intent(requireContext(), CatalogActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(logTag, "onResume - this tab is now visible")
    }

    override fun onPause() {
        super.onPause()
        Log.d(logTag, "onPause")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear the binding here or the view hierarchy leaks.
        Log.d(logTag, "onDestroyView - releasing the binding")
        _binding = null
    }
}
