package com.example.runway.ui.auth

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.runway.R
import com.example.runway.RunwayApplication
import com.example.runway.data.auth.AuthSession
import com.example.runway.databinding.FragmentSigninBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.android.material.switchmaterial.SwitchMaterial
import androidx.biometric.BiometricManager

/** Google sign-in screen with validation, safe errors and session persistence. */
class SignInFragment : Fragment() {

    private var _binding: FragmentSigninBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val application get() = requireActivity().application as RunwayApplication
    private val sessionStore get() = application.container.authSessionStore
    private val googleAuthClient get() = application.container.googleAuthClient
    private var biometricAvailable = false

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        _binding?.authGoogleButton?.isEnabled = true
        if (_binding == null) return@registerForActivityResult
        if (result.resultCode != Activity.RESULT_OK) {
            showError(getString(R.string.rw_auth_cancelled))
            return@registerForActivityResult
        }

        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                .getResult(ApiException::class.java)
            val id = account.id
            val email = account.email
            if (id.isNullOrBlank() || email.isNullOrBlank()) {
                showError(getString(R.string.rw_auth_invalid_account))
                return@registerForActivityResult
            }

            sessionStore.saveSession(
                AuthSession(
                    id = id,
                    email = email,
                    displayName = account.displayName.orEmpty(),
                    photoUrl = account.photoUrl?.toString(),
                    signedInAtMillis = System.currentTimeMillis(),
                    biometricEnabled = binding.authFingerprintSwitch.isChecked && biometricAvailable,
                )
            )
            val destination = if (binding.authFingerprintSwitch.isChecked && biometricAvailable) {
                R.id.action_signIn_to_biometric
            } else {
                R.id.action_signIn_to_home
            }
            findNavController().navigate(destination)
        } catch (_: ApiException) {
            showError(getString(R.string.rw_auth_sign_in_failed))
        } catch (_: Exception) {
            showError(getString(R.string.rw_auth_sign_in_failed))
        }
    }

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
        binding.authEmailField.placeholder = getString(R.string.rw_auth_email_hint)
        binding.authEmailField.editText.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        biometricAvailable = isBiometricAvailable()
        binding.authFingerprintSwitch.isChecked = biometricAvailable
        binding.authFingerprintSwitch.isEnabled = biometricAvailable

        binding.authGoogleButton.setOnClickListener {
            binding.authEmailField.error = null
            binding.authError.isVisible = false
            val emailHint = binding.authEmailField.text.trim()
            if (emailHint.isNotBlank() && !Patterns.EMAIL_ADDRESS.matcher(emailHint).matches()) {
                binding.authEmailField.error = getString(R.string.rw_auth_email_error)
                return@setOnClickListener
            }

            hideKeyboard()
            binding.authGoogleButton.isEnabled = false
            try {
                googleSignInLauncher.launch(
                    googleAuthClient.buildSignInIntent(emailHint.takeIf { it.isNotBlank() })
                )
            } catch (_: Exception) {
                binding.authGoogleButton.isEnabled = true
                showError(getString(R.string.rw_auth_sign_in_failed))
            }
        }
    }

    private fun hideKeyboard() {
        val inputManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE)
            as? InputMethodManager ?: return
        inputManager.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    private fun showError(message: String) {
        if (_binding == null) return
        binding.authError.text = message
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
