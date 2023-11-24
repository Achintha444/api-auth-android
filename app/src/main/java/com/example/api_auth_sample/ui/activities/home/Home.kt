package com.example.api_auth_sample.ui.activities.home

import CardAdapter
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.api_auth_sample.ui.activities.main.MainActivity
import com.example.api_auth_sample.R
import com.example.api_auth_sample.api.DataSource
import com.example.api_auth_sample.databinding.ActivityHomeBinding
import com.example.api_auth_sample.util.UiUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

class Home : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var signoutButton: ImageButton
    private lateinit var doctorsRecyclerView: RecyclerView
    private lateinit var pharmacysRecyclerView: RecyclerView
    private lateinit var petsRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeComponents()

        // hide action bar and status bar
        UiUtil.hideStatusBar(window, resources, theme, R.color.asgardeo_secondary)

        val accessToken = UiUtil.readFromSharedPreferences(
            applicationContext.getSharedPreferences(
                R.string.app_name.toString(), Context.MODE_PRIVATE
            ), "accessToken"
        ).toString()

        setDoctorsCardAdapter()
        setPharmacysCardAdapter()

        signoutButton.setOnClickListener{
            GoogleSignIn.getClient(this, GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build())
                .signOut()
            val intent = Intent(this@Home, MainActivity::class.java)
            startActivity(intent)
        }
    }

    private fun initializeComponents() {
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        signoutButton = findViewById(R.id.signOutButton)
        doctorsRecyclerView = findViewById(R.id.doctorsRecyledView)
        pharmacysRecyclerView = findViewById(R.id.pharmacysRecylerView)
//        petsRecyclerView = findViewById(R.id.petsRecylerView)

    }

    private fun setDoctorsCardAdapter() {
        val cardAdapter = CardAdapter(DataSource.getDummyDoctors())
        doctorsRecyclerView.layoutManager = LinearLayoutManager(this)
        doctorsRecyclerView.adapter = cardAdapter
    }

    private fun setPharmacysCardAdapter() {
        val cardAdapter = CardAdapter(DataSource.getDummyPharmacies())
        pharmacysRecyclerView.layoutManager = LinearLayoutManager(this)
        pharmacysRecyclerView.adapter = cardAdapter
    }
}