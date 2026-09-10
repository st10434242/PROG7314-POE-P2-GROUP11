package com.example.runway.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.biometric.BiometricManager
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.runway.R
import com.example.runway.RunwayApplication
import com.example.runway.data.auth.SignInOutcome
import com.example.runway.databinding.FragmentSigninBinding
import kotlinx.coroutines.launch

/** Google sign-in screen with safe errors and session persistence. */
class SignInFragment : Fragment() {

    private var _binding: FragmentSigninBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val application get() = requireActivity().application as RunwayApplication
    private val sessionStore get() = application.container.authSessionStore
    private val googleAuthClient get() = application.container.googleAuthClient
    private var biometricAvailable = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSigninBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        biometricAvailable = isBiometricAvailable()
        binding.authFingerprintSwitch.isChecked = biometricAvailable
        binding.authFingerprintSwitch.isEnabled = biometricAvailable

        binding.authGoogleButton.setOnClickListener { startSignIn() }
    }

    private fun startSignIn() {
        binding.authError.isVisible = false
        setBusy(true)

        viewLifecycleOwner.lifecycleScope.launch {
            // requireActivity() rather than the fragment: the account sheet needs a window.
            val outcome = googleAuthClient.signIn(requireActivity())
            if (_binding == null) return@launch
            setBusy(false)

            when (outcome) {
                is SignInOutcome.Success -> onSignedIn(outcome)
                SignInOutcome.Failure.Cancelled -> showError(R.string.rw_auth_cancelled)
                SignInOutcome.Failure.NoAccount -> showError(R.string.rw_auth_no_account)
                SignInOutcome.Failure.Configuration -> showError(R.string.rw_auth_config_error)
                SignInOutcome.Failure.Network -> showError(R.string.rw_auth_network_error)
                SignInOutcome.Failure.IncompleteAccount ->
                    showError(R.string.rw_auth_invalid_account)
                SignInOutcome.Failure.Failed -> showError(R.string.rw_auth_sign_in_failed)
            }
        }
    }

    private fun onSignedIn(outcome: SignInOutcome.Success) {
        val useBiometric = binding.authFingerprintSwitch.isChecked && biometricAvailable
        sessionStore.saveSession(outcome.session.copy(biometricEnabled = useBiometric))

        val destination = if (useBiometric) {
            R.id.action_signIn_to_biometric
        } else {
            R.id.action_signIn_to_home
        }
        findNavController().navigate(destination)
    }

    private fun setBusy(busy: Boolean) {
        binding.authGoogleButton.isEnabled = !busy
    }

    private fun showError(messageRes: Int) {
        if (_binding == null) return
        binding.authError.setText(messageRes)
        binding.authError.isVisible = true
    }

    private fun isBiometricAvailable(): Boolean = BiometricManager.from(requireContext())
        .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
        BiometricManager.BIOMETRIC_SUCCESS

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}