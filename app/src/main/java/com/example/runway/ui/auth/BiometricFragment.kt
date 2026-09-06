package com.example.runway.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.example.runway.R
import com.example.runway.RunwayApplication
import com.example.runway.databinding.FragmentBiometricBinding

/** Fingerprint/face unlock screen matching the returning-user mockup. */
class BiometricFragment : Fragment() {

    private var _binding: FragmentBiometricBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val application get() = requireActivity().application as RunwayApplication
    private var authenticationInProgress = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentBiometricBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val session = application.container.authSessionStore.currentSession
        if (session == null) {
            goToSignIn()
            return
        }

        binding.biometricAvatar.text = initials(session.displayName, session.email)
        binding.biometricEmail.text = session.email
        binding.biometricButton.setOnClickListener { authenticate() }
        binding.biometricUseAccount.setOnClickListener { goToSignIn() }

        if (isBiometricAvailable()) authenticate()
        else showError(getString(R.string.rw_auth_fingerprint_unavailable))
    }

    private fun authenticate() {
        if (!isAdded || _binding == null || authenticationInProgress ||
            !lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        ) return
        authenticationInProgress = true
        val executor = ContextCompat.getMainExecutor(requireContext())
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                authenticationInProgress = false
                navigateSafely(R.id.action_biometric_to_home)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                authenticationInProgress = false
                if (errorCode != BiometricPrompt.ERROR_CANCELED && errorCode != BiometricPrompt.ERROR_USER_CANCELED) {
                    showError(getString(R.string.rw_biometric_failed))
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                showError(getString(R.string.rw_biometric_failed))
            }
        })
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.rw_biometric_title))
                .setSubtitle(getString(R.string.rw_biometric_helper))
                .setNegativeButtonText(getString(R.string.rw_biometric_use_account))
                .build()
        )
    }

    private fun isBiometricAvailable(): Boolean = BiometricManager.from(requireContext())
        .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
        BiometricManager.BIOMETRIC_SUCCESS

    private fun goToSignIn() {
        authenticationInProgress = false
        navigateSafely(R.id.action_biometric_to_signIn)
    }

    private fun navigateSafely(actionId: Int) {
        if (!isAdded || !lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
        val navController = findNavController()
        if (navController.currentDestination?.id != R.id.biometricFragment) return
        runCatching { navController.navigate(actionId) }
    }

    private fun showError(message: String) {
        _binding?.biometricError?.text = message
    }

    private fun initials(displayName: String, email: String): String {
        val source = displayName.ifBlank { email.substringBefore('@') }
        return source.split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .take(2)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
