package com.example.api_auth_sample.ui.activities.login

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.example.api_auth_sample.R
import com.example.api_auth_sample.databinding.ActivityLoginBinding
import com.example.api_auth_sample.model.data.authenticator.Authenticator
import com.example.api_auth_sample.controller.ui.activities.fragments.auth.authMethods.AuthenticatorFragment
import com.example.api_auth_sample.model.data.authenticator.AuthenticatorType
import com.example.api_auth_sample.ui.activities.login.fragments.auth.AuthFragment
import com.example.api_auth_sample.util.UiUtil

class Login : AppCompatActivity(), AuthFragment.AuthListener {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var stepString: String
    private lateinit var flowId: String
    private lateinit var fragmentManager: FragmentManager
    private lateinit var authFragment: AuthFragment
    private lateinit var bundle: Bundle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setAuthenticatorsString()

        initalizeAuthFragment()

        initializeComponents()

        // hide action bar and status bar
        UiUtil.hideStatusBar(window, resources, theme, R.color.font_white)
    }

    private fun initializeComponents() {
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    private fun setAuthenticatorsString() {
        val intent: Intent = intent
        stepString = intent.getStringExtra("step")!!
        flowId = intent.getStringExtra("flowId")!!
    }

    private fun initalizeAuthFragment() {
        fragmentManager = supportFragmentManager
        val mFragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
        authFragment = AuthFragment()

        bundle = Bundle()
        bundle.putString("stepString", stepString)
        bundle.putString("flowId", flowId)

        authFragment.arguments = bundle
        mFragmentTransaction.add(R.id.authLayoutView, authFragment).commit()
    }

    override fun onAuthenticatorPassed(authenticator: Authenticator) {

        val authFragment: Fragment? = fragmentManager.findFragmentById(R.id.authLayoutView);
        val authChildFragmentManager: FragmentManager? = authFragment!!.childFragmentManager;
        lateinit var authView: AuthenticatorFragment

        when (authenticator.authenticator) {
            AuthenticatorType.BASIC.authenticator -> {
                authView =
                    authChildFragmentManager!!.findFragmentById(R.id.basicAuthView) as AuthenticatorFragment
            }

            AuthenticatorType.PASSKEY.authenticator -> {
                authView =
                    authChildFragmentManager!!.findFragmentById(R.id.fidoAuthView) as AuthenticatorFragment
            }

            AuthenticatorType.TOTP.authenticator -> {
                authView =
                    authChildFragmentManager!!.findFragmentById(R.id.totpIdpView) as AuthenticatorFragment
            }

            AuthenticatorType.GOOGLE.authenticator -> {
                authView =
                    authChildFragmentManager!!.findFragmentById(R.id.googleIdpView) as AuthenticatorFragment
            }
        }

        authView.updateAuthenticator(authenticator);
    }
}