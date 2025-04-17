package com.example.mobile_lab_android

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.mobile_lab_android.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        replaceFragment(ProductsFragment())
        updateSelectedTab(binding.btnProducts)

        binding.btnProfile.setOnClickListener {
            replaceFragment(ProfileFragment())
            updateSelectedTab(binding.btnProfile)
        }

        binding.btnProducts.setOnClickListener {
            replaceFragment(ProductsFragment())
            updateSelectedTab(binding.btnProducts)
        }

        binding.btnFavorites.setOnClickListener {
            replaceFragment(FavoritesFragment())
            updateSelectedTab(binding.btnFavorites)
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun updateSelectedTab(selectedView: TextView) {
        resetTabStyles()

        selectedView.setBackgroundResource(R.drawable.selected_tab_background)
        selectedView.setTextColor(resources.getColor(android.R.color.white))
    }

    private fun resetTabStyles() {
        val tabs = listOf(binding.btnProfile, binding.btnProducts, binding.btnFavorites)
        for (tab in tabs) {
            tab.background = null
            tab.setTextColor(resources.getColor(R.color.gray))
        }
    }
}