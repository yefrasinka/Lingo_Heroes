package com.example.lingoheroesapp.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.lingoheroesapp.R
import com.example.lingoheroesapp.fragments.FriendRequestsFragment
import com.example.lingoheroesapp.fragments.FriendsListFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class FriendsActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friends)

        setupViews()
        setupViewPager()
        setupTabLayout()
    }

    private fun setupViews() {
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)

        val backButton = findViewById<ImageView>(R.id.backButton)
        backButton.setOnClickListener {
            finish()
        }

        val searchButton = findViewById<ImageView>(R.id.searchButton)
        searchButton.setOnClickListener {
            startActivity(Intent(this, UserSearchActivity::class.java))
        }
    }

    private fun setupViewPager() {
        val pagerAdapter = FriendsPagerAdapter(this)
        viewPager.adapter = pagerAdapter
    }

    private fun setupTabLayout() {
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Znajomi"
                1 -> "Zaproszenia"
                else -> null
            }
        }.attach()
    }

    private inner class FriendsPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> FriendsListFragment()
                1 -> FriendRequestsFragment()
                else -> throw IllegalArgumentException("Invalid position: $position")
            }
        }
    }
}