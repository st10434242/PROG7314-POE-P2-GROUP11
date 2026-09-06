package com.example.runway

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.runway.databinding.ActivityMainBinding
import com.example.runway.ui.components.RunwayToast

/**
 * The app's single Activity. Hosts the nav graph and owns the bottom bar,
 * the add-item FAB and the sync banner.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")

        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyWindowInsets()
        setUpNavigation()
        setUpFab()
    }

    /** Connects the bottom navigation bar to the nav graph. */
    private fun setUpNavigation() {
        val host = supportFragmentManager.findFragmentById(R.id.navHost) as NavHostFragment
        navController = host.navController
        binding.bottomNav.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            Log.d(TAG, "navigated to ${destination.label ?: destination.id}")
        }
    }

    /** The centre FAB opens the add-item flow, which is not a tab. */
    private fun setUpFab() {
        binding.addFab.setOnClickListener {
            Log.d(TAG, "add-item FAB tapped")
            // Placeholder until the add-item flow is built.
            RunwayToast.show(
                view = binding.root,
                message = getString(R.string.rw_stub_add),
                anchor = binding.bottomNav,
            )
        }
    }

    /** Pads the layout so the system bars do not draw over it. */
    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.mainRoot) { view, windowInsets ->
            val bars = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            // Top inset goes on the root, not the sync banner, because the banner is usually hidden.
            view.updatePadding(top = bars.top, left = bars.left, right = bars.right)
            binding.bottomNav.updatePadding(bottom = bars.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy - isFinishing=$isFinishing")
    }

    private companion object {
        const val TAG = "MainActivity"
    }
}
