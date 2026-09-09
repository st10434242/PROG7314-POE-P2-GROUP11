package com.example.runway.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.runway.R
import com.example.runway.RunwayApplication
import com.example.runway.databinding.FragmentProfileBinding
import com.example.runway.ui.components.RunwayDialogs
import com.example.runway.ui.components.RunwayToast

/** Profile tab with the session logout action. */
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val application get() = requireActivity().application as RunwayApplication

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val session = application.container.authSessionStore.currentSession
        binding.profileEmail.text = session?.email ?: getString(R.string.rw_auth_no_session)
        binding.profileSignOutButton.setOnClickListener {
            RunwayDialogs.confirm(
                context = requireContext(),
                titleRes = R.string.rw_profile_sign_out_title,
                bodyRes = R.string.rw_profile_sign_out_body,
                confirmLabelRes = R.string.rw_profile_sign_out,
                destructive = true,
            ) {
                signOut()
            }
        }
    }

    private fun signOut() {
        application.container.authSessionStore.clearSession()
        application.container.googleAuthClient.signOut().addOnCompleteListener { task ->
            val currentBinding = _binding
            if (!task.isSuccessful && currentBinding != null) {
                RunwayToast.show(currentBinding.root, R.string.rw_auth_sign_out_failed)
            }
            if (isAdded) {
                findNavController().navigate(R.id.action_profile_signOut_to_signIn)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
